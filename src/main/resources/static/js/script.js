document.addEventListener('DOMContentLoaded', () => {
  // Sidebar Toggle Functionality
  const toggleMenuBtn = document.getElementById('toggleMenuBtn');
  const appContainer = document.getElementById('appContainer');
  if (toggleMenuBtn && appContainer) {
    toggleMenuBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      // On mobile screens, toggle sidebar-open
      if (window.innerWidth <= 768) {
        appContainer.classList.toggle('sidebar-open');
      } else {
        // On desktop screens, toggle sidebar-collapsed
        appContainer.classList.toggle('sidebar-collapsed');
      }
    });
  }
  // Close sidebar when clicking outside on mobile view
  document.addEventListener('click', (e) => {
    if (window.innerWidth <= 768 && appContainer && appContainer.classList.contains('sidebar-open')) {
      const sidebar = document.querySelector('.sidebar');
      if (sidebar && !sidebar.contains(e.target) && !toggleMenuBtn.contains(e.target)) {
        appContainer.classList.remove('sidebar-open');
      }
    }
  });
  // Navigation Items Active State
  const navItems = document.querySelectorAll('.sidebar-nav .nav-item');
  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      // Allow link default action if navigating pages
      navItems.forEach(nav => nav.classList.remove('active'));
      item.classList.add('active');
      const titleElement = document.querySelector('.page-title');
      const navText = item.querySelector('span:not(.icon)')?.textContent;
      if (titleElement && navText) {
        titleElement.textContent = navText;
      }
    });
  });
  // Authentication Form Simulation (Login / Signup Toggle)
  const authForm = document.getElementById('authForm');
  if (authForm) {
    authForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const emailInput = document.getElementById('emailInput')?.value;
      const passwordInput = document.getElementById('passwordInput')?.value;
      if (!emailInput || !passwordInput) {
        showToast('Por favor, preencha todos os campos.');
        return;
      }
      showToast('Autenticando...');
      setTimeout(() => {
        window.location.href = '/dashboard';
      }, 600);
    });
  }
  // Register link toggle in Auth Page
  const toggleAuthModeBtn = document.getElementById('toggleAuthMode');
  if (toggleAuthModeBtn) {
    toggleAuthModeBtn.addEventListener('click', (e) => {
      e.preventDefault();
      const authTitle = document.getElementById('authTitle');
      const authSubmitBtn = document.getElementById('authSubmitBtn');
      const authSubtitle = document.getElementById('authSubtitle');
      const authFooterText = document.getElementById('authFooterText');
      if (authTitle.textContent === 'Entrar na conta') {
        authTitle.textContent = 'Criar conta';
        authSubtitle.textContent = 'Cadastre-se para acessar e gerenciar o EduGrid.';
        authSubmitBtn.textContent = 'Cadastrar';
        authFooterText.innerHTML = 'Já tem uma conta? <a href="#" id="toggleAuthMode">Entrar</a>';
      } else {
        authTitle.textContent = 'Entrar na conta';
        authSubtitle.textContent = 'Acesse o painel e gerencie suas grades horárias.';
        authSubmitBtn.textContent = 'Entrar';
        authFooterText.innerHTML = 'Não tem conta? <a href="#" id="toggleAuthMode">Cadastre-se</a>';
      }

      // Re-attach event listener to newly rendered link
      document.getElementById('toggleAuthMode')?.addEventListener('click', arguments.callee);
    });
  }
  // Action Buttons Interactivity
  const actionButtons = document.querySelectorAll('.banner-action-btn, .link-btn, .help-fab');
  actionButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const btnText = btn.textContent.trim();
      if (btnText.includes('Gerar Grade')) {
        showToast('✨ Gerador de Grade iniciado com sucesso!');
      } else if (btnText.includes('Ver tod')) {
        showToast('Carregando lista completa...');
      } else if (btn.classList.contains('help-fab')) {
        showToast(' Central de ajuda EduGrid');
      }
    });
  });
});

document.getElementById('menuDashboard')?.addEventListener('click', function (e) {
  showToast('Navegando para Dashboard...');
  e.preventDefault();
  window.location.href = '/dashboard';
});

document.getElementById('menuProfessores')?.addEventListener('click', function (e) {
  showToast('Navegando para Professores...');
  e.preventDefault();
  window.location.href = '/professor/professor';

});
document.getElementById('menuTurmas')?.addEventListener('click', function (e) {
  showToast('Navegando para Turmas...');
  e.preventDefault();
  window.location.href = '/turma/turma';
});

document.getElementById('menuDisciplinas')?.addEventListener('click', function (e) {
  showToast('Navegando para Disciplinas...');
  e.preventDefault();
  window.location.href = '/disciplina/disciplina';
});

document.getElementById('menuGerarGrade')?.addEventListener('click', function (e) {
  showToast('Navegando para Gerar Grade...');
  e.preventDefault();
  window.location.href = '/gradeHoraria/gerar';
});

document.getElementById('addProfessorBtn')?.addEventListener('click', function (e) {
  showToast('Abrindo formulário para adicionar professor...');
  e.preventDefault();
  window.location.href = '/professor/salvar';
});

document.getElementById('addTurmaBtn')?.addEventListener('click', function (e) {
  showToast('Abrindo formulário para adicionar turma...');
  e.preventDefault();
  window.location.href = '/turma/salvar';
});

document.getElementById('addDisciplineBtn')?.addEventListener('click', function (e) {
  showToast('Abrindo formulário para adicionar disciplina...');
  e.preventDefault();
  window.location.href = '/disciplina/salvar';
});

document.getElementById('editProfessorBtn')?.addEventListener('click', function (e) {
  showToast('Abrindo formulário para editar professor...');
  e.preventDefault();
  window.location.href = '/professor/atualizar';
});

document.getElementById('editTurmaBtn')?.addEventListener('click', function (e) {
  showToast('Abrindo formulário para editar turma...');
  e.preventDefault();
  window.location.href = '/turma/atualizar';
});

document.getElementById('editDisciplineBtn')?.addEventListener('click', function (e) {
  showToast('Abrindo formulário para editar disciplina...');
  e.preventDefault();
  window.location.href = '/disciplina/atualizar';
});

document.getElementById('deleteProfessorBtn')?.addEventListener('click', function (e) {
  showToast('Abrindo formulário para excluir professor...');
  e.preventDefault();
  window.location.href = '/professor/deletar';
});

document.getElementById('deleteTurmaBtn')?.addEventListener('click', function (e) {
  showToast('Abrindo formulário para excluir turma...');
  e.preventDefault();
  window.location.href = '/turma/deletar';
});

document.getElementById('deleteDisciplineBtn')?.addEventListener('click', function (e) {
  showToast('Abrindo formulário para excluir disciplina...');
  e.preventDefault();
  window.location.href = '/disciplina/deletar';
});




// Helper Function for Notification Toast
function showToast(message) {
  let toast = document.getElementById('toastNotification');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'toastNotification';
    toast.className = 'toast-notification';
    document.body.appendChild(toast);
  }
  toast.innerHTML = `<span>ℹ️</span> <div>${message}</div>`;
  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 3000);
}



document.addEventListener("DOMContentLoaded", () => {

    // ==========================
    // ELEMENTOS DA GRADE
    // ==========================

    const daysRange = document.getElementById("daysRange");
    const periodsRange = document.getElementById("periodsRange");

    const daysValText = document.getElementById("daysValText");
    const periodsValText = document.getElementById("periodsValText");

    const summaryDays = document.getElementById("summaryDays");
    const summaryPeriods = document.getElementById("summaryPeriods");
    const summaryTotal = document.getElementById("summaryTotal");

    const daysTicks = document.querySelectorAll("#daysTicks .tick-item");

    if (!daysRange || !periodsRange) {
      return;
    }

    // ==========================
    // ATUALIZAÇÃO DA GRADE
    // ==========================

    function atualizarGrade() {

        const dias = parseInt(daysRange.value);
        const periodos = parseInt(periodsRange.value);

        // Atualiza os valores próximos aos sliders
        daysValText.textContent = dias;
        periodsValText.textContent = periodos;

        // Atualiza o resumo
        summaryDays.textContent = dias;
        summaryPeriods.textContent = periodos;

        // Calcula quantidade total de aulas na semana
        const total = dias * periodos;

        summaryTotal.textContent = total;

        // Atualiza os dias da semana visualmente
        daysTicks.forEach((tick) => {

            const dia = parseInt(tick.dataset.day);

            if (dia <= dias) {
                tick.classList.add("active");
            } else {
                tick.classList.remove("active");
            }

        });
    }


    // ==========================
    // EVENTOS DOS SLIDERS
    // ==========================

    daysRange.addEventListener("input", atualizarGrade);
    periodsRange.addEventListener("input", atualizarGrade);


    // ==========================
    // VALORES INICIAIS
    // ==========================

    atualizarGrade();

});

function gerarGradeVisual() {
  const thead = document.getElementById("scheduleThead");
  const tbody = document.getElementById("scheduleTbody");
  const totalPeriodos = quantidadeDias * periodosPorDia;

  if (!thead || !tbody || !totalPeriodos) {
    return;
  }

  thead.innerHTML = "";
  tbody.innerHTML = "";

  const headerRow = document.createElement("tr");
  const turmaHeader = document.createElement("th");
  turmaHeader.textContent = "Turma";
  headerRow.appendChild(turmaHeader);

  for (let periodo = 0; periodo < totalPeriodos; periodo++) {
    const th = document.createElement("th");
    th.textContent = `Período ${periodo + 1}`;
    headerRow.appendChild(th);
  }

  thead.appendChild(headerRow);

  const matriz = Array.isArray(matrizGrade) ? matrizGrade : [];
  const turmas = Array.isArray(turmasGrade) ? turmasGrade : [];
  const quantidadeTurmas = Math.max(matriz.length, turmas.length);

  for (let indiceTurma = 0; indiceTurma < quantidadeTurmas; indiceTurma++) {
    const row = document.createElement("tr");
    const turmaHeaderCell = document.createElement("th");
    turmaHeaderCell.scope = "row";
    turmaHeaderCell.classList.add("schedule-row-header");
    turmaHeaderCell.textContent = turmas[indiceTurma]?.nome || `Turma ${indiceTurma + 1}`;
    row.appendChild(turmaHeaderCell);

    const linha = Array.isArray(matriz[indiceTurma]) ? matriz[indiceTurma] : [];

    for (let periodo = 0; periodo < totalPeriodos; periodo++) {
      const cell = document.createElement("td");
      cell.classList.add("schedule-cell");
      cell.dataset.turma = indiceTurma;
      cell.dataset.periodo = periodo;

      const aula = linha[periodo];
      const possuiAula = aula !== null && aula !== undefined && aula !== "";
      const conteudo = document.createElement("div");
      const descricaoAula = typeof aula === "object" && aula !== null
        ? aula.nome || aula.disciplina || JSON.stringify(aula)
        : possuiAula ? String(aula) : "—";

      conteudo.classList.add(possuiAula ? "lesson-content" : "empty-cell");
      conteudo.textContent = descricaoAula;
      cell.appendChild(conteudo);
      row.appendChild(cell);
    }

    tbody.appendChild(row);
  }
}

document.addEventListener("DOMContentLoaded", () => {
if (typeof quantidadeDias !== "undefined" && quantidadeDias > 0 &&
  typeof periodosPorDia !== "undefined" && periodosPorDia > 0) {
    gerarGradeVisual();
}

});