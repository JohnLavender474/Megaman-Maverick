#!/usr/bin/env bash
cmd=$(jq -r '.tool_input.command // empty')
[ -z "$cmd" ] && exit 0

flat=$(printf '%s' "$cmd" | tr '\n' ' ')

reason=""
if printf '%s' "$flat" | grep -Eq '(^|[;&|({[:space:]])(until|while)[[:space:]].*[[:space:];]sleep[[:space:]]'; then
  reason="a wait loop with sleep"
elif printf '%s' "$flat" | grep -Eq '(^|[;&|({[:space:]])pgrep[[:space:]][^;|&]*-[a-zA-Z]*f'; then
  reason="pgrep -f (it also matches the shell whose command line holds the pattern)"
fi

[ -z "$reason" ] && exit 0

jq -n --arg r "Blocked: $reason. Do not poll. Run a long command with run_in_background and wait for its completion notification, or run it in the foreground with a timeout. To find a process, list PIDs with ps and check /proc/<pid>/cwd." '{
  hookSpecificOutput: {
    hookEventName: "PreToolUse",
    permissionDecision: "deny",
    permissionDecisionReason: $r
  }
}'
