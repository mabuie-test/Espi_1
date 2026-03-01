# Configuração mínima (Android + PHP/MySQL)

## 1) Painel PHP + MySQL

1. Crie banco e tabelas:
   ```bash
   mysql -u root -p < web_panel/sql/schema.sql
   ```
2. Ajuste credenciais em `web_panel/src/config.php`.
3. Crie usuário inicial com hash seguro (`password_hash`).
4. Publique `web_panel/public` em servidor com HTTPS obrigatório.
5. Garanta permissão de escrita em `web_panel/storage/uploads`.

## 2) App Android (AIDE)

1. Importe a pasta `android_app` no AIDE.
2. Adicione JAR/AAR locais em `android_app/libs/`.
3. Atualize URLs em `StreamClient`, `UploadManager`, `AuthManager`, `CommandClient`.
4. Abra app e faça:
   - ativar consentimento (persistido uma única vez)
   - ativar admin device
   - ativar toggle de controlo remoto (opcional)
5. Se quiser comando remoto, os 3 pré-requisitos acima devem estar ativos.

## 3) Fluxo de controle remoto seguro

1. App autentica no painel e usa `device_token` estável.
2. Painel envia comando start/stop com fonte (`Frontal`, `Traseira`, `Microfone principal`) para `/api/device_command.php`.
3. App consulta comandos periodicamente e executa apenas se consentimento+admin+controle remoto estiverem ativos.
4. App envia eventos para `/api/stream_ingest.php` e upload final para `/api/upload.php`.

## 4) Segurança recomendada

- HTTPS em todos endpoints.
- Trocar `jwt_secret` por segredo forte no servidor.
- Rotação de tokens e expiração curta.
- Logging e monitoramento ativo de comandos.
