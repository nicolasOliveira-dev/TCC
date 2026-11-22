document.addEventListener('DOMContentLoaded', () => {
  const toggleMenuBtn = document.getElementById('toggleMenuBtn');
  const appContainer = document.getElementById('appContainer');
  const printButtons = [
    document.getElementById('btnPrintHorario'),
    document.getElementById('btnPrint')
  ];
  printButtons.forEach(button => {
    button?.addEventListener('click', () => window.print());
  });
  if (toggleMenuBtn && appContainer) {
    toggleMenuBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      if (window.innerWidth <= 768) {
        appContainer.classList.toggle('sidebar-open');
      } else {
        appContainer.classList.toggle('sidebar-collapsed');
      }
    });
  }
  document.addEventListener('click', (e) => {
    if (window.innerWidth <= 768 && appContainer && appContainer.classList.contains('sidebar-open')) {
      const sidebar = document.querySelector('.sidebar');
      if (sidebar && !sidebar.contains(e.target) && !toggleMenuBtn.contains(e.target)) {
        appContainer.classList.remove('sidebar-open');
      }
    }
  });
  const navItems = document.querySelectorAll('.sidebar-nav .nav-item');
  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      navItems.forEach(nav => nav.classList.remove('active'));
      item.classList.add('active');
      const titleElement = document.querySelector('.page-title');
      const navText = item.querySelector('span:not(.icon)')?.textContent;
      if (titleElement && navText) {
        titleElement.textContent = navText;
      }
    });
  });
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

      document.getElementById('toggleAuthMode')?.addEventListener('click', arguments.callee);
    });
  }
  const actionButtons = document.querySelectorAll('.banner-action-btn, .link-btn, .help-fab');
  actionButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const btnText = btn.textContent.trim();
      if (btnText.includes('Gerar Grade')) {
        window.location.href = '/gradeHoraria/gerar';
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

document.getElementById('menuCursos')?.addEventListener('click', function (e) {
  showToast('Navegando para Cursos...');
  e.preventDefault();
  window.location.href = '/curso/curso';
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

  const daysRange = document.getElementById("daysRange");
  const periodsRange = document.getElementById("periodsRange");
  const shiftsRange = document.getElementById("shiftsRange");

  const daysValText = document.getElementById("daysValText");
  const periodsValText = document.getElementById("periodsValText");
  const shiftsValText = document.getElementById("shiftsValText");

  const summaryDays = document.getElementById("summaryDays");
  const summaryPeriods = document.getElementById("summaryPeriods");
  const summaryShifts = document.getElementById("summaryShifts");
  const summaryTotal = document.getElementById("summaryTotal");

  const daysTicks = document.querySelectorAll("#daysTicks .tick-item");
  const shiftsTicks = document.querySelectorAll("#shiftsTicks .tick-item");

  if (!daysRange || !periodsRange) {
    return;
  }

  function atualizarGrade() {

    const dias = parseInt(daysRange.value);
    const periodos = parseInt(periodsRange.value);
    const turnos = parseInt(shiftsRange.value);

    daysValText.textContent = dias;
    periodsValText.textContent = periodos;
    shiftsValText.textContent = turnos;

    summaryDays.textContent = dias;
    summaryPeriods.textContent = periodos;
    summaryShifts.textContent = turnos;

    const total = dias * periodos;

    summaryTotal.textContent = total;

    shiftsTicks.forEach((tick) => {
      const turno = parseInt(tick.dataset.shift);
      tick.classList.toggle("active", turno <= turnos);
    });

    daysTicks.forEach((tick) => {

      const dia = parseInt(tick.dataset.day);

      if (dia <= dias) {
          tick.classList.add("active");
      } else {
          tick.classList.remove("active");
      }

    });
  }

  daysRange.addEventListener("input", atualizarGrade);
  periodsRange.addEventListener("input", atualizarGrade);
  shiftsRange.addEventListener("input", atualizarGrade);

  atualizarGrade();

});

function abreviarDisciplina(nome) {
  const letras = String(nome ?? "").match(/\p{L}/gu) || [];
  return letras.slice(0, 3).join("").toLocaleUpperCase("pt-BR");
}

function gerarGradeVisual() {
  const container = document.getElementById("scheduleTablesContainer");
  const totalPeriodos = quantidadeDias * periodosPorDia;

  if (!container || !totalPeriodos) {
    return;
  }

  container.innerHTML = "";
  const diasDaSemana = [
    "Segunda-feira",
    "Terça-feira",
    "Quarta-feira",
    "Quinta-feira",
    "Sexta-feira",
    "Sábado",
    "Domingo"
  ];
  const matriz = Array.isArray(matrizGrade) ? matrizGrade : [];
  const turmas = Array.isArray(turmasGrade) ? turmasGrade : [];
  const turnos = Array.isArray(turnosTurmas) ? turnosTurmas : [];
  const registrosHorario = Array.isArray(horariosGrade) ? horariosGrade : [];
  const horariosPorPosicao = new Map();
  registrosHorario.forEach((horario) => {
    const chave = `${horario.turmaId}:${horario.dia}:${horario.periodo}`;
    const registros = horariosPorPosicao.get(chave) || [];
    registros.push(horario);
    horariosPorPosicao.set(chave, registros);
  });
  const quantidadeTurmas = Math.max(matriz.length, turmas.length);
  const nomesTurnos = ["Manhã", "Tarde", "Noite"];
  const gruposTurnos = Array.from(
    { length: Math.max(Number(quantidadeTurnos) || 0, 1) },
    (_, indice) => ({ nome: nomesTurnos[indice] || `Turno ${indice + 1}`, turmas: [] })
  );
  const turmasSemTurnoIdentificado = [];

  for (let indiceTurma = 0; indiceTurma < quantidadeTurmas; indiceTurma++) {
    const indiceTurno = Number.isInteger(turnos[indiceTurma])
      ? turnos[indiceTurma]
      : -1;

    if (indiceTurno < 0) {
      turmasSemTurnoIdentificado.push(indiceTurma);
      continue;
    }

    while (gruposTurnos.length <= indiceTurno) {
      const proximoIndice = gruposTurnos.length;
      gruposTurnos.push({
        nome: nomesTurnos[proximoIndice] || `Turno ${proximoIndice + 1}`,
        turmas: []
      });
    }
    gruposTurnos[indiceTurno].turmas.push(indiceTurma);
  }

  if (turmasSemTurnoIdentificado.length) {
    gruposTurnos.push({ nome: "Turno não identificado", turmas: turmasSemTurnoIdentificado });
  }

  gruposTurnos.forEach((grupo) => {
    const turnoSection = document.createElement("section");
    turnoSection.classList.add("schedule-shift");

    const turnoHeading = document.createElement("h3");
    turnoHeading.classList.add("schedule-shift-heading");
    turnoHeading.textContent = grupo.nome;
    turnoSection.appendChild(turnoHeading);

    const tableWrapper = document.createElement("div");
    tableWrapper.classList.add("schedule-table-wrapper");
    const table = document.createElement("table");
    table.classList.add("schedule-table", "matrix-table");

    const thead = document.createElement("thead");
    const dayHeaderRow = document.createElement("tr");
    const periodHeaderRow = document.createElement("tr");
    const turmaHeader = document.createElement("th");
    turmaHeader.textContent = "Turma";
    turmaHeader.rowSpan = 2;
    turmaHeader.scope = "col";
    dayHeaderRow.appendChild(turmaHeader);

    for (let dia = 0; dia < quantidadeDias; dia++) {
      const thDia = document.createElement("th");
      thDia.colSpan = periodosPorDia;
      thDia.scope = "colgroup";
      thDia.textContent = diasDaSemana[dia] || `Dia ${dia + 1}`;
      dayHeaderRow.appendChild(thDia);

      for (let periodo = 0; periodo < periodosPorDia; periodo++) {
        const th = document.createElement("th");
        th.scope = "col";
        th.textContent = `Período ${periodo + 1}`;
        periodHeaderRow.appendChild(th);
      }
    }
    thead.append(dayHeaderRow, periodHeaderRow);
    table.appendChild(thead);

    const tbody = document.createElement("tbody");
    grupo.turmas.forEach((indiceTurma) => {
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
        cell.tabIndex = 0;
        cell.setAttribute("role", "gridcell");
        cell.dataset.turma = indiceTurma;
        cell.dataset.periodo = periodo;
        const turmaId = turmas[indiceTurma]?.id;
        const diaSemana = Math.floor(periodo / periodosPorDia);
        const periodoDia = periodo % periodosPorDia;
        const registros = horariosPorPosicao.get(`${turmaId}:${diaSemana}:${periodoDia}`) || [];
        const registro = registros[0];
        cell.dataset.turmaId = turmaId ?? "";
        cell.dataset.dia = diaSemana;
        cell.dataset.periodoDia = periodoDia;
        if (registro) {
          cell.dataset.horarioId = registro.id;
          cell.dataset.turno = registro.turno;
          const aulasNoPeriodo = registros.map(horario =>
            `${horario.disciplina}${horario.professor ? ` - ${horario.professor}` : ""}`
          );
          cell.title = aulasNoPeriodo.join(" | ");
          if (registros.length > 1) {
            cell.classList.add("schedule-cell-conflict");
          } else {
            cell.draggable = true;
          }
        }

        const aula = linha[periodo];
        const possuiAula = registros.length > 0 || (aula !== null && aula !== undefined && aula !== "");
        const conteudo = document.createElement("div");
        const nomeDaAula = typeof aula === "object" && aula !== null
          ? aula.nome || aula.disciplina || JSON.stringify(aula)
          : possuiAula ? String(aula) : "-";
        const disciplinasNaCelula = registros.length
          ? registros.map(horario => horario.disciplina)
          : [nomeDaAula];
        const descricaoAula = disciplinasNaCelula.join(" / ");
        const textoDaCelula = possuiAula
          ? disciplinasNaCelula.map(abreviarDisciplina).join(" / ")
          : "-";

        conteudo.classList.add(possuiAula ? "lesson-content" : "empty-cell");
        conteudo.textContent = textoDaCelula;
        conteudo.title = descricaoAula;
        cell.setAttribute("aria-label", `${turmas[indiceTurma]?.nome || "Turma"}, ${descricaoAula}, dia ${diaSemana + 1}, período ${periodoDia + 1}`);
        cell.appendChild(conteudo);
        row.appendChild(cell);
      }
      tbody.appendChild(row);
    });

    table.appendChild(tbody);
    tableWrapper.appendChild(table);
    turnoSection.appendChild(tableWrapper);
    container.appendChild(turnoSection);
  });
}

document.addEventListener("DOMContentLoaded", () => {
  if (typeof quantidadeDias !== "undefined" && quantidadeDias > 0 &&
    typeof periodosPorDia !== "undefined" && periodosPorDia > 0) {
    gerarGradeVisual();
  }

  const botaoEditar = document.getElementById("btnEditSchedule");
  const botaoDesfazer = document.getElementById("btnUndoEdit");
  const painel = document.getElementById("sideInstructionCard");
  const container = document.getElementById("scheduleTablesContainer");
  if (!botaoEditar || !container || !painel) return;
  const chaveDesfazer = `edugrid-undo-${gradeId}`;
  if (botaoDesfazer && sessionStorage.getItem(chaveDesfazer)) botaoDesfazer.hidden = false;
  let modoEdicao = false;
  let origem = null;
  const dica = painel.querySelector("p");
  let previa = document.getElementById("editPreview");
  if (!previa) { previa = document.createElement("div"); previa.id = "editPreview"; previa.className = "edit-preview"; painel.appendChild(previa); }
  botaoEditar.addEventListener("click", () => {
    modoEdicao = !modoEdicao; origem = null;
    limparSelecao();
    botaoEditar.setAttribute("aria-pressed", String(modoEdicao));
    botaoEditar.textContent = modoEdicao ? "Concluir edição" : "Editar grade";
    container.classList.toggle("schedule-editing", modoEdicao);
    if (dica) dica.textContent = modoEdicao
      ? "Selecione um bloco e arraste até outro horário. Clique em um bloco e depois no destino para mover ou trocar."
      : "Clique em “Editar grade” para movimentar blocos e conferir as restrições.";
    previa.textContent = "";
  });
  function limparSelecao() { container.querySelectorAll(".edit-selected,.edit-target").forEach(c => c.classList.remove("edit-selected", "edit-target")); }
  function moverPara(destino) {
    if (!modoEdicao || !origem || !destino) return;
    if (origem.dataset.turmaId !== destino.dataset.turmaId) { previa.textContent = "Mova ou troque blocos dentro da mesma turma."; return; }
    const payload = { turmaId: Number(origem.dataset.turmaId), diaOrigem: Number(origem.dataset.dia),
      periodoOrigem: Number(origem.dataset.periodoDia), diaDestino: Number(destino.dataset.dia),
      periodoDestino: Number(destino.dataset.periodoDia), confirmarSoft: false };
    previa.textContent = "Verificando as restrições…";
    fetch(`/gradeHoraria/grade/${gradeId}/prever-edicao`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) })
      .then(async response => ({ ok: response.ok, data: await response.json() }))
      .then(({ok, data}) => {
        limparSelecao();
        const restricoes = [...(data.restricoesHard || []).map(x => `Obrigatória: ${x}`), ...(data.restricoesSoft || []).map(x => `Flexível: ${x}`)];
        previa.textContent = restricoes.length ? restricoes.join(" | ") : data.mensagem;
        if (!ok || !data.valido) return;
        const salvar = () => fetch(`/gradeHoraria/grade/${gradeId}/edicoes`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({...payload, confirmarSoft: true}) })
          .then(async response => ({ok: response.ok, data: await response.json()}))
          .then(result => { previa.textContent = result.data.mensagem + ((result.data.restricoesSoft || []).length ? ` Avisos: ${result.data.restricoesSoft.join(" | ")}` : ""); if (result.ok && result.data.salvo) { sessionStorage.setItem(chaveDesfazer, JSON.stringify(payload)); if (botaoDesfazer) botaoDesfazer.hidden = false; window.location.reload(); } });
        salvar();
      }).catch(() => { previa.textContent = "Não foi possível validar o movimento. Tente novamente."; });
    origem = null;
  }
  container.addEventListener("click", event => {
    if (!modoEdicao) return;
    const cell = event.target.closest(".schedule-cell");
    if (!cell) return;
    event.preventDefault();
    if (!origem) { if (!cell.dataset.horarioId) return; origem = cell; cell.classList.add("edit-selected"); }
    else moverPara(cell);
  });
  container.addEventListener("keydown", event => {
    if (!modoEdicao || (event.key !== "Enter" && event.key !== " ")) return;
    const cell = event.target.closest(".schedule-cell");
    if (!cell) return;
    event.preventDefault();
    cell.click();
  });
  container.addEventListener("dragstart", event => {
    if (!modoEdicao) { event.preventDefault(); return; }
    const cell = event.target.closest(".schedule-cell");
    if (!cell?.dataset.horarioId) { event.preventDefault(); return; }
    origem = cell; event.dataTransfer.setData("text/plain", cell.dataset.horarioId);
    setTimeout(() => cell.classList.add("edit-selected"), 0);
  });
  container.addEventListener("dragover", event => { if (modoEdicao && event.target.closest(".schedule-cell")) event.preventDefault(); });
  container.addEventListener("drop", event => { if (!modoEdicao) return; const cell = event.target.closest(".schedule-cell"); if (cell) { event.preventDefault(); moverPara(cell); } });
  botaoDesfazer?.addEventListener("click", async () => {
    const ultima = JSON.parse(sessionStorage.getItem(chaveDesfazer) || "null");
    if (!ultima) return;
    previa.textContent = "Desfazendo a última alteração…";
    try {
      const response = await fetch(`/gradeHoraria/grade/${gradeId}/edicoes`, { method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ turmaId: ultima.turmaId, diaOrigem: ultima.diaDestino, periodoOrigem: ultima.periodoDestino,
          diaDestino: ultima.diaOrigem, periodoDestino: ultima.periodoOrigem, confirmarSoft: true }) });
      const result = await response.json();
      previa.textContent = result.mensagem;
      if (response.ok && result.salvo) { sessionStorage.removeItem(chaveDesfazer); window.location.reload(); }
    } catch { previa.textContent = "Não foi possível desfazer. Recarregue a grade e tente novamente."; }
  });

});
