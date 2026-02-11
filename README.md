# Connector Guard Rails

This server now enforces a strict contract for connector calls and response shapes.

## Request shape
- `POST /connector/invoke`
- Body: `{ "tool": "<name>", "args": { ... }, "userId": "...", "sessionId": "...", "systemPrompt": "<short string>" }`
- System prompts are capped at 1000 characters, scanned for forbidden UI-driving tokens, optionally matched against an allowlist, and logged (sanitized). Rejected prompts return `400`.
- A short enforced guard is always prepended before forwarding: `Return strictly structured JSON with fields meta and data. Avoid narrative.`

## Downstream payload
- Forwarded payload always carries `system_prompt`, `tool`, `args`, `user_id`, `session_id`.
- User/session IDs stay intact for correlation.

## Canonical response contract
- Every connector response is normalized to `{ "meta": {...}, "data": <object|list>, "diagnostics": {...} }`.
- `meta` hints only (client owns presentation). Fields: `intent`, `view` (one of `summary`, `detailed`, `compact`), `count`, `highlight_ids`, `next_step`, `source`.
- `diagnostics` holds internal details such as latency, request_id, and guard information. No user-facing narrative is allowed.
- Narrative fields (`assistant_text`, `message`, `narrative`, `text`) are rejected outright.

## Mapping rules (centralized in `ConnectorResponseMapper`)
- `list_courses` → `meta.intent=list_courses`, `meta.view=summary`, `meta.count` equals list size, `meta.source=list_courses`.
- `get_course_details` → `meta.intent=course_details`, `meta.view=detailed`, `meta.highlight_ids=[<course id>]`, `meta.source=get_course_details`.
- Unknown shapes are wrapped with `meta.view=summary` and `meta.intent=unknown`; narrative fields are blocked.

## Tool metadata via YAML
- Tool descriptions, meta hints, system prompt controls, and instructions live in `src/main/resources/tools.yaml`.
- Root keys:
  - `system_prompt`: enforced guard, max length, forbidden/allowed tokens.
  - `instructions`: MCP/server instructions used for the system message.
- Each tool entry has `name`, `description`, and optional `meta` map.
- Function tool registrations read from this YAML to populate descriptions; extend the file to add or adjust tooling metadata.
- MCP tool responses are wrapped through the normalizer so ChatGPT clients receive `{meta,data,diagnostics}` automatically; YAML `meta` entries overlay the generated meta (e.g., `next_step`).

## Observability and safety
- Metrics: `connector.calls.total`, `connector.calls.throttled`, `connector.prompts.rejected`, `connector.downstream.latency`.
- Simple per-user/session throttle window is applied and logged when tripped.
- Sanitized prompts are logged; rejected prompts are logged for auditing.

## Operational reminders
- Keep UI wording on the client; `meta` is guidance only.
- Keep the enforced guard consistent across services.
- Reject any downstream narrative fields; do not pass them through.
- Mapping and validation live in `com.gc.mcp.server.connector` for easy auditing and extension.
