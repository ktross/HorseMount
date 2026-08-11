#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <server-jar> <plugin-jar> <server-name>" >&2
  exit 2
fi

if [[ "${MINECRAFT_EULA:-}" != "TRUE" ]]; then
  echo "Set the MINECRAFT_EULA repository variable to TRUE only after accepting the Minecraft EULA." >&2
  exit 2
fi

server_jar="$(realpath "$1")"
plugin_jar="$(realpath "$2")"
server_name="$3"
server_dir="$(mktemp -d "${RUNNER_TEMP:-/tmp}/horsemount-${server_name}-XXXXXX")"
server_pid=""

print_log() {
  if [[ -f "$server_dir/server.log" ]]; then
    echo "----- $server_name server log -----" >&2
    tail -n 200 "$server_dir/server.log" >&2
  fi
}

has_compatibility_error() {
  grep -Eq "Error occurred while (enabling|disabling) HorseMount|Could not load ['\"]?plugins/HorseMount|NoClassDefFoundError|NoSuchMethodError|AbstractMethodError" \
    "$server_dir/server.log"
}

stop_server() {
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
    printf 'stop\n' >&3 || true
    for _ in {1..60}; do
      if ! kill -0 "$server_pid" 2>/dev/null; then
        break
      fi
      sleep 1
    done
    if kill -0 "$server_pid" 2>/dev/null; then
      kill "$server_pid" 2>/dev/null || true
    fi
    wait "$server_pid" 2>/dev/null || true
  fi
}

trap stop_server EXIT

mkdir -p "$server_dir/plugins"
cp "$server_jar" "$server_dir/server.jar"
cp "$plugin_jar" "$server_dir/plugins/HorseMount.jar"
printf 'eula=true\n' > "$server_dir/eula.txt"
cat > "$server_dir/server.properties" <<'PROPERTIES'
online-mode=false
max-players=1
spawn-protection=0
view-distance=2
simulation-distance=2
PROPERTIES
mkfifo "$server_dir/server-input"

cd "$server_dir"
exec 3<>server-input
java -Xms512M -Xmx1G -jar server.jar --nogui <&3 >server.log 2>&1 &
server_pid=$!

ready=false
deadline=$((SECONDS + 480))
while ((SECONDS < deadline)); do
  if grep -Eq 'Done \([^)]+\)!' server.log; then
    ready=true
    break
  fi
  if ! kill -0 "$server_pid" 2>/dev/null; then
    echo "$server_name exited before startup completed." >&2
    print_log
    exit 1
  fi
  sleep 2
done

if [[ "$ready" != true ]]; then
  echo "$server_name did not finish starting within 480 seconds." >&2
  print_log
  exit 1
fi

if ! grep -Fq 'Enabling HorseMount v' server.log; then
  echo "$server_name reached ready state without enabling HorseMount." >&2
  print_log
  exit 1
fi

if has_compatibility_error; then
  echo "$server_name reported a HorseMount compatibility error." >&2
  print_log
  exit 1
fi

printf 'version HorseMount\n' >&3
version_reported=false
deadline=$((SECONDS + 30))
while ((SECONDS < deadline)); do
  if grep -Fq 'HorseMount version ' server.log; then
    version_reported=true
    break
  fi
  sleep 1
done

if [[ "$version_reported" != true ]]; then
  echo "$server_name did not report the loaded HorseMount version." >&2
  print_log
  exit 1
fi

printf 'stop\n' >&3
for _ in {1..120}; do
  if ! kill -0 "$server_pid" 2>/dev/null; then
    break
  fi
  sleep 1
done

if kill -0 "$server_pid" 2>/dev/null; then
  echo "$server_name did not stop cleanly." >&2
  print_log
  exit 1
fi

wait "$server_pid"
server_pid=""

if has_compatibility_error; then
  echo "$server_name reported a HorseMount compatibility error during shutdown." >&2
  print_log
  exit 1
fi

trap - EXIT
echo "$server_name loaded HorseMount and stopped cleanly."
