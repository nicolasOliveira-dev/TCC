document.addEventListener('DOMContentLoaded', () => {
  const areaAtuacao = document.getElementById('areaAtuacao');
  const opcoes = document.querySelectorAll('.discipline-option');
  const mensagemVazia = document.querySelector('[data-disciplinas-empty]');

  if (!areaAtuacao) {
    return;
  }

  const normalizar = valor => valor
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toLowerCase();

  const filtrarDisciplinas = () => {
    const area = normalizar(areaAtuacao.value);
    let quantidadeVisivel = 0;

    opcoes.forEach(opcao => {
      const corresponde = area !== ''
        && normalizar(opcao.dataset.disciplinaArea || '') === area;
      opcao.hidden = !corresponde;
      if (!corresponde) {
        opcao.querySelector('input').checked = false;
      } else {
        quantidadeVisivel++;
      }
    });

    if (mensagemVazia) {
      mensagemVazia.hidden = quantidadeVisivel > 0;
    }
  };

  areaAtuacao.addEventListener('input', filtrarDisciplinas);
  filtrarDisciplinas();
});
