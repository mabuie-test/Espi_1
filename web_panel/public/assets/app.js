(async function () {
  const cmdForm = document.getElementById('cmdForm');
  const cmdResult = document.getElementById('cmdResult');
  const livePlayer = document.getElementById('livePlayer');
  const healthBadge = document.getElementById('healthBadge');
  const watchBtn = document.getElementById('watchLiveBtn');
  const liveVideo = document.getElementById('liveVideo');
  const liveAudio = document.getElementById('liveAudio');
  const liveHint = document.getElementById('liveHint');
  let liveTimer = null;

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

  function updateLive(deviceId) {
    const ts = Date.now();
    const src = '/live_media.php?device_id=' + encodeURIComponent(deviceId) + '&t=' + ts;
    liveVideo.src = src;
    liveAudio.src = src;
    liveHint.style.display = 'none';
    liveVideo.style.display = 'block';
    liveAudio.style.display = 'block';
    liveVideo.load();
    liveAudio.load();
  }

  watchBtn?.addEventListener('click', function () {
    const deviceId = Number(document.getElementById('device_id').value);
    if (!deviceId) {
      cmdResult.textContent = 'Selecione um dispositivo para acompanhar.';
      return;
    }
    if (liveTimer) clearInterval(liveTimer);
    updateLive(deviceId);
    liveTimer = setInterval(() => updateLive(deviceId), 3000);
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
