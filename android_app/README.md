# Android (Java, AIDE-compatible)

Este módulo foi estruturado para **Android 8.0+** com foco em gravação eficiente, transparente e com consentimento persistido.

## Dependências locais (AIDE)
Coloque os arquivos em `android_app/libs/`:

- `java-websocket-1.5.3.jar`
- `json-20231013.jar` (se necessário)

## Recursos
- Captura vídeo+áudio ou apenas áudio.
- Serviço foreground com notificação persistente.
- Pausar/retomar/parar.
- Upload por chunks com retry/backoff.
- Sinalização em tempo real (WebSocket) + metadados (memória/estado/fonte).
- Polling de comandos remotos (iniciar/parar) **somente se o usuário ativar explicitamente**.
- Seleção de fonte: `Frontal`, `Traseira`, `Microfone principal`.

## Consentimento e admin device
- Consentimento é persistido e dado uma única vez (`setConsentOnce`).
- Comandos remotos só são aceitos se: **consentimento + toggle remoto ativo + admin device ativo**.
- Privilégios admin são usados de forma transparente para robustez operacional, sem captura oculta.

## Configuração rápida
- Centralize URLs em `ServerConfig.java` para evitar inconsistências entre classes de rede.

## Endpoints esperados
- `POST https://SEU_HOST/api/login.php`
- `POST https://SEU_HOST/api/upload.php`
- `GET  https://SEU_HOST/api/device_command.php`
- `POST https://SEU_HOST/api/device_command.php`
- `POST https://SEU_HOST/api/device_register.php`
- `POST https://SEU_HOST/api/stream_ingest.php`
- `wss://SEU_HOST/ws/stream.php`


## Importante (app)
Antes de iniciar gravação, use o botão **Conectar ao servidor** (URL + utilizador + senha) para obter JWT e registrar o dispositivo.
