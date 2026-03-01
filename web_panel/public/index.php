<?php
require __DIR__ . '/../src/bootstrap.php';

$active = 0;
$uploads = 0;
$recent = [];
$dbWarning = null;

try {
    $active = db()->query("SELECT COUNT(*) total FROM media_sessions WHERE stream_status IN ('connected','resumed')")->fetch()['total'] ?? 0;
    $uploads = db()->query('SELECT COALESCE(SUM(bytes_received),0) AS total_bytes FROM media_sessions')->fetch()['total_bytes'] ?? 0;
    $recent = db()->query('SELECT file_name, media_type, stream_status, bytes_received, created_at FROM media_sessions ORDER BY id DESC LIMIT 20')->fetchAll();
} catch (Throwable $e) {
    $dbWarning = 'Banco indisponível para o preview local.';
}
?>
<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>ESPI Painel</title>
  <link rel="stylesheet" href="/assets/style.css">
</head>
<body>
  <main>
    <h1>Dashboard de Streaming</h1>
    <?php if ($dbWarning): ?><p class="warn"><?= htmlspecialchars($dbWarning) ?></p><?php endif; ?>
    <section class="cards">
      <article><h2>Câmeras ativas</h2><p><?= (int)$active ?></p></article>
      <article><h2>Bytes recebidos</h2><p><?= number_format((int)$uploads, 0, ',', '.') ?></p></article>
    </section>

    <section>
      <h2>Sessões recentes</h2>
      <table>
        <thead><tr><th>Arquivo</th><th>Tipo</th><th>Status</th><th>Bytes</th><th>Data</th></tr></thead>
        <tbody>
          <?php foreach ($recent as $row): ?>
          <tr>
            <td><?= htmlspecialchars($row['file_name']) ?></td>
            <td><?= htmlspecialchars($row['media_type']) ?></td>
            <td><?= htmlspecialchars($row['stream_status']) ?></td>
            <td><?= (int)$row['bytes_received'] ?></td>
            <td><?= htmlspecialchars($row['created_at']) ?></td>
          </tr>
          <?php endforeach; ?>
        </tbody>
      </table>
    </section>

    <section>
      <h2>Arquivos gravados</h2>
      <ul>
        <?php
          $files = glob(__DIR__ . '/../storage/uploads/*.{mp4,m4a,part}', GLOB_BRACE) ?: [];
          foreach ($files as $file):
              $name = basename($file);
        ?>
          <li>
            <?= htmlspecialchars($name) ?>
            <?php if (preg_match('/\.mp4$/', $name)): ?>
                <video controls width="360" src="/media.php?f=<?= urlencode($name) ?>"></video>
            <?php else: ?>
                <audio controls src="/media.php?f=<?= urlencode($name) ?>"></audio>
            <?php endif; ?>
          </li>
        <?php endforeach; ?>
      </ul>
    </section>
  </main>
</body>
</html>
