# GitHub App Exchange Server (Local)

Minimal backend for Painkiller's GitHub App sign-in flow.

## Endpoint

- `POST /github-app/exchange`
- request body:

```json
{ "installation_id": "12345678" }
```

- success response:

```json
{ "token": "ghs_...", "expires_at": "2026-04-27T12:00:00Z" }
```

## Required env vars

- `APP_ID` — GitHub App ID
- one of:
  - `APP_PRIVATE_KEY` (PEM content as env value; `\n` supported), or
  - `APP_PRIVATE_KEY_PATH` (path to `.pem` file)

Optional:

- `PORT` (default `3000`)

## Install & run

```bash
cd tools/github-app-exchange-server
npm install
APP_ID=123456 APP_PRIVATE_KEY_PATH=~/github-app-private-key.pem npm start
```

Or use helper script:

```bash
cd tools/github-app-exchange-server
./start-local.sh 123456 ~/github-app-private-key.pem 3000
```
