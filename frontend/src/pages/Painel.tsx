import { useQuery } from '@tanstack/react-query';
import { get, talvez } from '../api/client';
import type { Configuracao, Insumo, ItemAbaixoMinimo, Pagina, Producao, ReceitaResumo, Tarifa } from '../api/types';
import { useUsuario } from '../auth/sessao';
import { Cabecalho, Carregando, ErroCarregar } from '../components/ui';
import { brl, dataHora, num, unidade } from '../lib/format';

export function Painel() {
  const usuario = useUsuario();
  const q = useQuery({
    queryKey: ['painel'],
    queryFn: async () => {
      const [config, tarifa, insumos, receitas, minimo, producoes] = await Promise.all([
        get<Configuracao>('/configuracao'),
        talvez(get<Tarifa>('/configuracao/tarifas/vigente')),
        get<Pagina<Insumo>>('/insumos?size=1'),
        get<Pagina<ReceitaResumo>>('/receitas?size=6'),
        get<ItemAbaixoMinimo[]>('/estoque/abaixo-minimo'),
        get<Pagina<Producao>>('/producoes?size=5'),
      ]);
      return { config, tarifa, insumos, receitas, minimo, producoes };
    },
  });
  if (q.isPending) return <Carregando />;
  if (q.isError) return <ErroCarregar erro={q.error} />;
  const { config, tarifa, insumos, receitas, minimo, producoes } = q.data;

  const passos: [boolean, string, string, string | null][] = [
    [Number(config.salarioDesejadoMes) > 0, '#/configuracao', 'Diga quanto quer ganhar por mês e quantas horas trabalha', 'Isso define o valor da sua hora.'],
    [Boolean(tarifa), '#/configuracao', 'Cadastre a tarifa de energia da sua conta de luz', 'Necessária se a receita usa equipamento elétrico.'],
    [insumos.totalElements > 0, '#/insumos', 'Cadastre os insumos com o preço que você paga', null],
    [receitas.totalElements > 0, '#/receitas/nova', 'Monte sua primeira receita e veja o preço', null],
  ];
  const pendentes = passos.filter(([feito]) => !feito).length;

  return (
    <div>
      <Cabecalho titulo={`Olá, ${usuario.nome.split(' ')[0]}`}
        descricao={`Sua hora vale ${brl(config.valorHora)}. Bandeira de energia vigente: ${config.bandeiraVigente.replace('_', ' ').toLowerCase()}.`} />

      {pendentes ? (
        <section className="painel">
          <h2>{pendentes === passos.length ? 'Comece por aqui' : `Faltam ${pendentes} ${pendentes === 1 ? 'passo' : 'passos'} para o primeiro preço`}</h2>
          <ol className="passos">
            {passos.map(([feito, href, texto, dica]) => (
              <li key={texto} className={feito ? 'feito' : undefined}>
                <div><a href={href}>{texto}</a>{dica ? <div className="small muted">{dica}</div> : null}</div>
              </li>
            ))}
          </ol>
        </section>
      ) : null}

      <div className="colunas bloco">
        <section className="painel">
          <h2>Receitas</h2>
          {receitas.content.length ? (
            <ul className="lista-simples">
              {receitas.content.map((r) => (
                <li key={r.id}>
                  <a href={`#/receitas/${r.id}`}>{r.nome}</a>
                  <span className="muted small num">{r.tamanhos.length ? `${r.tamanhos.length} tamanhos` : `rende ${r.rendimento}`}</span>
                </li>
              ))}
            </ul>
          ) : <p className="muted">Nenhuma receita ainda.</p>}
          <p className="bloco"><a className="botao" href="#/receitas">Ver todas</a></p>
        </section>
        <section className="painel">
          <h2>Comprar logo</h2>
          {minimo.length ? (
            <ul className="lista-simples">
              {minimo.slice(0, 6).map((i) => (
                <li key={i.id}>
                  <span>{i.nome}</span>
                  <span className="num">{num(i.saldo, 3)} {unidade(i.unidade)} <span className="selo atencao">mín. {num(i.estoqueMinimo, 3)}</span></span>
                </li>
              ))}
            </ul>
          ) : <p className="muted">Nenhum item abaixo do estoque mínimo.</p>}
          <p className="bloco"><a className="botao" href="#/estoque">Abrir estoque</a></p>
        </section>
        <section className="painel">
          <h2>Últimas produções</h2>
          {producoes.content.length ? (
            <ul className="lista-simples">
              {producoes.content.map((p) => (
                <li key={p.id}>
                  <span>{p.receita}{p.tamanho ? ` (${p.tamanho})` : ''}<div className="small muted">{dataHora(p.criadoEm)}</div></span>
                  <span className="num">{p.unidadesProduzidas} un</span>
                </li>
              ))}
            </ul>
          ) : <p className="muted">Nenhuma produção registrada.</p>}
          <p className="bloco"><a className="botao" href="#/producoes">Ver produções</a></p>
        </section>
      </div>
    </div>
  );
}
