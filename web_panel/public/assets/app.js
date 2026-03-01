(async function () {
  const cmdForm = document.getElementById('cmdForm');
  const cmdResult = document.getElementById('cmdResult');
  const picker = document.getElementById('sessionPicker');
  const livePlayer = document.getElementById('livePlayer');

  function renderMedia(name) {
    if (!name) {
      livePlayer.innerHTML = '<p>Escolha uma sessão para acompanhar a transmissão/arquivo.</p>';
      return;
    }
    const ext = name.split('.').pop().toLowerCase();
    const src = '/media.php?f=' + encodeURIComponent(name);
    if (ext === 'mp4') {
      livePlayer.innerHTML = '<video controls autoplay muted width="100%" src="' + src + '"></video>';
    } else {
      livePlayer.innerHTML = '<audio controls autoplay src="' + src + '"></audio>';
    }
  }

  picker?.addEventListener('change', function () {
    renderMedia(this.value);
  });

  cmdForm?.addEventListener('submit', async function (e) {
    e.preventDefault();
    const payload = {
      device_id: Number(document.getElementById('device_id').value),
      action: document.getElementById('action').value,
      mode: document.getElementById('mode').value,
      source: document.getElementById('source').value
    };

    if (!payload.device_id) {
      cmdResult.textContent = 'Selecione um dispositivo.';
      return;
    }

    try {
      const r = await fetch('/api/device_command.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const j = await r.json();
      cmdResult.textContent = j.ok ? 'Comando enviado com sucesso.' : 'Falha ao enviar comando.';
    } catch (err) {
      cmdResult.textContent = 'Erro de rede ao enviar comando.';
    }
  });
})();
