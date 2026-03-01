# Android (Java, AIDE-compatible)

Este módulo foi estruturado para **Android 8.0+** com foco em gravação eficiente, transparente e com consentimento.

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

## Privacidade
- Sem ativação oculta.
- Sem uso de privilégios de admin de dispositivo para controle silencioso.
- Controle remoto condicionado a consentimento local.

## Endpoints esperados
- `POST https://SEU_HOST/api/login.php`
- `POST https://SEU_HOST/api/upload.php`
- `GET  https://SEU_HOST/api/device_command.php`
- `POST https://SEU_HOST/api/device_command.php`
- `POST https://SEU_HOST/api/stream_ingest.php`
- `wss://SEU_HOST/ws/stream.php`
