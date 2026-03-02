<?php
require __DIR__ . '/../src/bootstrap.php';

$userId = require_web_session();

$active = 0;
$uploads = 0;
$recent = [];
$devices = [];
$dbWarning = null;

try {
    $active = db()->prepare("SELECT COUNT(*) total FROM media_sessions WHERE user_id = ? AND stream_status IN ('connected','resumed','heartbeat')");
    $active->execute([$userId]);
    $active = $active->fetch()['total'] ?? 0;
    $uploads = db()->prepare('SELECT COALESCE(SUM(bytes_received),0) AS total_bytes FROM media_sessions WHERE user_id = ?');
    $uploads->execute([$userId]);
    $uploads = $uploads->fetch()['total_bytes'] ?? 0;
    $recentStmt = db()->prepare('SELECT ms.id, ms.file_name, ms.media_type, ms.source_name, ms.stream_status, ms.bytes_received, ms.created_at, d.device_name FROM media_sessions ms LEFT JOIN devices d ON d.id = ms.device_id WHERE ms.user_id = ? ORDER BY ms.id DESC LIMIT 20');
    $recentStmt->execute([$userId]);
    $recent = $recentStmt->fetchAll();
    $devicesStmt = db()->prepare('SELECT id, device_name, online_status, last_seen FROM devices WHERE user_id = ? ORDER BY id DESC');
    $devicesStmt->execute([$userId]);
    $devices = $devicesStmt->fetchAll();
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
    <header class="hero">
      <a class="logout" href="/logout.php">Sair</a>
      <h1>🎥 ESPI Control Center</h1>
      <p>Acompanhe transmissões ao vivo, escolha a fonte e envie comandos para dispositivos online com consentimento do utilizador.</p>
      <p id="healthBadge" class="health">Estado do painel: a verificar...</p>
    </header>

    <?php if ($dbWarning): ?><p class="warn"><?= htmlspecialchars($dbWarning) ?></p><?php endif; ?>

    <section class="cards">
      <article><h2>Dispositivos/sessões ativas</h2><p><?= (int)$active ?></p></article>
      <article><h2>Bytes recebidos</h2><p><?= number_format((int)$uploads, 0, ',', '.') ?></p></article>
      <article><h2>Dispositivos registados</h2><p><?= count($devices) ?></p></article>
    </section>

    <section class="panel-grid">
      <div class="panel">
        <h2>Controle de transmissão</h2>
        <form id="cmdForm">
          <label>Dispositivo</label>
          <select id="device_id" required>
            <option value="">Selecione</option>
            <?php foreach ($devices as $d): ?>
              <option value="<?= (int)$d['id'] ?>"><?= htmlspecialchars($d['device_name']) ?> <?= (int)$d['online_status'] ? '🟢' : '⚪' ?></option>
            <?php endforeach; ?>
          </select>

          <label>Ação</label>
          <select id="action"><option value="start">Iniciar</option><option value="stop">Parar</option></select>

          <label>Modo</label>
          <select id="mode"><option value="video_audio">Vídeo + Áudio</option><option value="audio_only">Apenas Áudio</option></select>

          <label>Fonte</label>
          <select id="source"><option>Frontal</option><option>Traseira</option><option>Microfone principal</option></select>

          <button type="submit">Enviar comando</button>
        </form>
        <p id="cmdResult"></p>
      </div>

      <div class="panel">
        <h2>Monitor ao vivo (dispositivo selecionado)</h2>
        <p>Selecione um dispositivo no painel de controlo e clique em acompanhar.</p>
        <button id="watchLiveBtn" type="button">Acompanhar ao vivo</button>
        <div id="livePlayer" class="live-player">
          <video id="liveVideo" controls autoplay muted width="100%" style="display:none"></video>
          <audio id="liveAudio" controls autoplay style="display:none"></audio>
          <p id="liveHint">Sem transmissão ativa.</p>
        </div>
      </div>
    </section>

    <section>
      <h2>Sessões recentes</h2>
      <table>
        <thead><tr><th>Dispositivo</th><th>Arquivo</th><th>Tipo</th><th>Fonte</th><th>Status</th><th>Bytes</th><th>Data</th></tr></thead>
        <tbody>
          <?php foreach ($recent as $row): ?>
          <tr>
            <td><?= htmlspecialchars((string)($row['device_name'] ?? '-')) ?></td>
            <td><?= htmlspecialchars($row['file_name']) ?></td>
            <td><?= htmlspecialchars($row['media_type']) ?></td>
            <td><?= htmlspecialchars((string)($row['source_name'] ?? 'auto')) ?></td>
            <td><span class="badge"><?= htmlspecialchars($row['stream_status']) ?></span></td>
            <td><?= (int)$row['bytes_received'] ?></td>
            <td><?= htmlspecialchars($row['created_at']) ?></td>
          </tr>
          <?php endforeach; ?>
        </tbody>
      </table>
    </section>
  </main>
  <script src="/assets/app.js"></script>
</body>
</html>
