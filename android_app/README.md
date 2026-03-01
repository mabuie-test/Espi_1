# Android (Java, AIDE-compatible)

Este módulo foi estruturado para **Android 8.0+** com foco em gravação eficiente e envio ao painel web.

## Dependências locais (AIDE)
Coloque os arquivos em `android_app/libs/`:

- `java-websocket-1.5.3.jar` (cliente WebSocket usado em `StreamClient`)
- `json-20231013.jar` (caso sua base AIDE não já inclua `org.json`)

> Sem Gradle online: use apenas JAR/AAR locais adicionados no projeto AIDE.

## Fluxo principal
1. Usuário abre app e concede permissões + consentimento explícito.
2. `RecordingService` inicia em foreground com notificação persistente.
3. Grava `video+audio` ou `audio-only` em bitrate otimizado.
4. `StreamClient` envia eventos de sessão em tempo real (WebSocket + JWT).
5. Ao parar, `UploadManager` realiza upload com retry e offset resumível via HTTP POST.

## Endpoints esperados
- Login JWT: `POST https://SEU_HOST/api/login.php`
- Upload chunk: `POST https://SEU_HOST/api/upload.php`
- Streaming WebSocket: `wss://SEU_HOST/ws/stream.php`

## Segurança e privacidade
- Consentimento explícito exigido antes de iniciar gravação.
- Notificação persistente enquanto gravação ativa.
- Recomendado forçar HTTPS e certificate pinning em produção.
