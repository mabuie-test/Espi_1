<?php
require __DIR__ . '/../src/bootstrap.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim((string)($_POST['username'] ?? ''));
    $password = (string)($_POST['password'] ?? '');

    $stmt = db()->prepare('SELECT id, password_hash FROM users WHERE username = ? LIMIT 1');
    $stmt->execute([$username]);
    $user = $stmt->fetch();

    if ($user && password_verify($password, $user['password_hash'])) {
        $_SESSION['uid'] = (int)$user['id'];
        $_SESSION['username'] = $username;
        header('Location: /');
        exit;
    }
    $error = 'Credenciais inválidas.';
}
?>
<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Login - ESPI</title>
  <link rel="stylesheet" href="/assets/style.css">
</head>
<body>
<main>
  <header class="hero"><h1>🔐 Login do Painel</h1><p>Acesso seguro ao centro de controlo.</p></header>
  <section class="panel" style="max-width:480px;margin:0 auto;">
    <?php if (!empty($error)): ?><p class="warn"><?= htmlspecialchars($error) ?></p><?php endif; ?>
    <form method="post">
      <label>Utilizador</label>
      <input name="username" required />
      <label>Senha</label>
      <input name="password" type="password" required />
      <button type="submit">Entrar</button>
    </form>
  </section>
</main>
</body>
</html>
