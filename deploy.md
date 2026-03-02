# Deploy (hospedagem com `public_html` fixo)

Este projeto foi ajustado para cenários onde **não é possível alterar o webroot**.

## Estrutura recomendada no servidor

Supondo `~/public_html` como raiz pública:

```text
/home/SEU_USUARIO/
  ├─ public_html/
  │   ├─ index.php        <- front controller (deste repositório)
  │   └─ .htaccess        <- rewrite para index.php
  └─ web_panel/
      ├─ public/
      ├─ src/
      ├─ sql/
      └─ storage/
```

> `web_panel` fica no **mesmo nível** de `public_html` (fora da web pública), como você descreveu.

## Passo a passo

1. Envie a pasta `web_panel/` para `~/web_panel`.
2. Envie os arquivos de `public_html/` (deste repo) para `~/public_html`.
3. No banco MySQL, rode:
   ```bash
   mysql -u USER -p DBNAME < web_panel/sql/schema.sql
   ```
4. Edite `web_panel/src/config.php` com credenciais corretas:
   - host, database, user, pass
   - `jwt_secret` forte
   - `base_url` real (ex.: `https://seu-dominio.com`)
5. Garanta permissão de escrita em `web_panel/storage/uploads`.

## Como funciona este ajuste

- `public_html/index.php` encaminha qualquer rota para `../web_panel/public`.
- `public_html/.htaccess` redireciona requisições não físicas para o front controller.
- Assim, rotas como abaixo continuam funcionando:
  - `/`
  - `/login.php`
  - `/api/login.php`
  - `/api/upload.php`
  - `/assets/style.css`

## Checklist rápido

- Se abrir tela em branco/500:
  - confira caminho real de `../web_panel/public`.
  - confira permissões de pasta/arquivo.
  - confira logs de erro do PHP no painel da hospedagem.
- Se APIs retornarem 500:
  - valide `web_panel/src/config.php`.
  - confirme extensão PDO/MySQL ativa.
- Se login não funcionar:
  - confirme usuário criado na tabela `users` com `password_hash`.

## Observação

Se seu provedor permitir `symlink`, você pode apontar `public_html` para `web_panel/public`, mas em hospedagem compartilhada geralmente isso é bloqueado. O front controller acima evita essa limitação.
