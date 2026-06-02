let device     = null;
let activeCall = null;

const statusEl  = document.getElementById('status');
const btnAnswer = document.getElementById('btnAnswer');
const btnHangup = document.getElementById('btnHangup');
const identEl   = document.getElementById('identity');

btnAnswer.addEventListener('click', answer);
btnHangup.addEventListener('click', hangup);

function setStatus(msg, bg = '#f1f5f9') {
  statusEl.textContent  = msg;
  statusEl.style.background = bg;
}

async function init() {
  setStatus('Récupération du token Twilio...');
  try {
    const res = await fetch('/api/crm/telephony/twilio/token/test', {
      credentials: 'include'
    });
    if (!res.ok) {
      throw new Error('Token introuvable — connecte-toi au CRM dans ce navigateur d\'abord.');
    }
    const data = await res.json();

    device = new Twilio.Device(data.token, { logLevel: 1 });

    device.on('registered', () => {
      setStatus('✅ Enregistré — en attente d\'appel entrant', '#dcfce7');
      identEl.textContent = 'Identité Twilio : test-receiver';
    });

    device.on('unregistered', () => {
      setStatus('Déconnecté', '#f1f5f9');
    });

    device.on('error', (err) => {
      setStatus('❌ Erreur : ' + err.message, '#fee2e2');
      console.error('[TestReceiver] Device error:', err);
    });

    device.on('incoming', (call) => {
      activeCall = call;
      setStatus('📞 Appel entrant — De : ' + (call.parameters.From || '?'), '#fef9c3');
      btnAnswer.style.display = 'inline-block';

      call.on('cancel', () => {
        setStatus('✅ Appel annulé — en attente', '#dcfce7');
        btnAnswer.style.display = 'none';
        activeCall = null;
      });
    });

    await device.register();

  } catch (e) {
    setStatus('❌ ' + e.message, '#fee2e2');
  }
}

function answer() {
  if (!activeCall) return;
  activeCall.accept();
  btnAnswer.style.display = 'none';
  btnHangup.style.display = 'inline-block';
  setStatus('🔊 En communication', '#dcfce7');

  activeCall.on('disconnect', () => {
    setStatus('✅ Appel terminé — en attente d\'appel entrant', '#dcfce7');
    btnHangup.style.display = 'none';
    activeCall = null;
  });
}

function hangup() {
  activeCall?.disconnect();
}

init();
