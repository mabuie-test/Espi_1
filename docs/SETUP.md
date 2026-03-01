# Configuração mínima (Android + PHP/MySQL)

## 1) Painel PHP + MySQL

1. Crie banco e tabelas:
   ```bash
   mysql -u root -p < web_panel/sql/schema.sql
   ```
2. Ajuste credenciais em `web_panel/src/config.php`.
3. Crie usuário inicial (exemplo):
   ```php
   <?php
   echo password_hash('SUA_SENHA_FORTE', PASSWORD_DEFAULT), PHP_EOL;
   ```
   Insira no MySQL em `users`.
4. Publique `web_panel/public` em servidor com HTTPS obrigatório.
5. Garanta permissão de escrita em `web_panel/storage/uploads`.

## 2) App Android (AIDE)

1. Importe a pasta `android_app` no AIDE.
2. Adicione JAR/AAR locais em `android_app/libs/`.
3. Atualize URLs em:
   - `StreamClient.java`
   - `UploadManager.java`
   - `AuthManager.java`
4. Instale em Android 8.0+ e conceda permissões.
5. Usuário deve tocar em **Dar Consentimento** antes de iniciar captura.

## 3) Segurança recomendada

- HTTPS em todos endpoints.
- Trocar `jwt_secret` por segredo forte no servidor.
- Rotação de tokens e expiração curta.
- Logs de eventos e monitoramento de uploads.

## 4) Observação operacional

- O projeto é um esqueleto profissional para consentimento explícito, notificação persistente e envio robusto (retry/resume).
- Para produção em larga escala, recomendam-se:
  - Worker queue para processamento de mídia.
  - WebSocket dedicado (ex.: Ratchet/Swoole ou gateway externo).
  - Transcodificação com FFmpeg em backend isolado.
