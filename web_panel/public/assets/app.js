(async function () {
  const cmdForm = document.getElementById('cmdForm');
  const cmdResult = document.getElementById('cmdResult');
  const picker = document.getElementById('sessionPicker');
  const livePlayer = document.getElementById('livePlayer');
  const healthBadge = document.getElementById('healthBadge');

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

  async function checkHealth() {
    if (!healthBadge) return;
    try {
      const r = await fetch('/api/health.php', { credentials: 'same-origin' });
      if (r.status === 401) {
        healthBadge.textContent = 'Estado do painel: sessão expirada';
        healthBadge.className = 'health health-warn';
        return;
      }
      const j = await r.json();
      if (j.ok) {
        healthBadge.textContent = 'Estado do painel: operacional';
        healthBadge.className = 'health health-ok';
      } else {
        healthBadge.textContent = 'Estado do painel: instável';
        healthBadge.className = 'health health-warn';
      }
    } catch (err) {
      healthBadge.textContent = 'Estado do painel: sem ligação';
      healthBadge.className = 'health health-warn';
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
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (r.status === 401) {
        cmdResult.textContent = 'Sessão expirada. Refaça login.';
        return;
      }
      const j = await r.json();
      cmdResult.textContent = j.ok ? 'Comando enviado com sucesso.' : ('Falha: ' + (j.error || 'erro desconhecido'));
    } catch (err) {
      cmdResult.textContent = 'Erro de rede ao enviar comando.';
    }
  });

  checkHealth();
  setInterval(checkHealth, 15000);
})();
