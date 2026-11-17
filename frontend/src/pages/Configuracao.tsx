import { useQuery, useQueryClient } from '@tanstack/react-query';
import { type FormEvent, useState } from 'react';
import { del, get, post, put, talvez } from '../api/client';
import type { AdicionalBandeira, Bandeira, Configuracao as Config, Pagina, Tarifa } from '../api/types';
import { useAviso } from '../components/Toasts';
import { Cabecalho, Campo, Carregando, Confirmar, ErroCarregar, ProblemaAlerta, erroDe, numero } from '../components/ui';
import { brl, data, hoje, num } from '../lib/format';

const BANDEIRAS: [Bandeira, string][] = [['VERDE', 'Verde'], ['AMARELA', 'Amarela'], ['VERMELHA_1', 'Vermelha patamar 1'], ['VERMELHA_2', 'Vermelha patamar 2']];
const nomeBandeira = (b: Bandeira) => BANDEIRAS.find(([v]) => v === b)?.[1] ?? b;
const reais6 = (v: number) => `R$ ${num(v, 6)}`;
const txt = (v: number | null | undefined) => (v == null ? '' : String(v));

export function Configuracao() {
  const q = useQuery({ queryKey: ['configuracao'], queryFn: () => get<Config>('/configuracao') });
  return (
    <div>
      <Cabecalho titulo="Meus custos" descricao="Valores que entram no preço de todas as receitas: seu trabalho, gás, energia e a divisão das contas do mês." />
      <section className="painel">
        <h2>Trabalho, gás e custos fixos</h2>
        {q.isPending ? <Carregando /> : q.isError ? <ErroCarregar erro={q.error} /> : <FormCustos config={q.data} />}
      </section>
      <div className="colunas bloco">
        <Tarifas />
        <Bandeiras />
      </div>
    </div>
  );
}

function FormCustos({ config }: { config: Config }) {
  const [valorHora, setValorHora] = useState(config.valorHora);
  const [erro, setErro] = useState<unknown>(null);
  const [enviando, setEnviando] = useState(false);
  const aviso = useAviso();
  const queryClient = useQueryClient();

  async function enviar(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    const fd = new FormData(ev.currentTarget);
    const f = Object.fromEntries(fd) as Record<string, string>;
    setEnviando(true);
    try {
      const r = await put<Config>('/configuracao', {
        salarioDesejadoMes: numero(f.salarioDesejadoMes), horasTrabalhadasMes: numero(f.horasTrabalhadasMes),
        gasPrecoBotijao: numero(f.gasPrecoBotijao), gasDuracaoHoras: numero(f.gasDuracaoHoras),
        bandeiraVigente: f.bandeiraVigente, criterioRateio: f.criterioRateio,
        permitirSaldoNegativo: fd.get('permitirSaldoNegativo') === 'on',
      });
      setValorHora(r.valorHora);
      setErro(null);
      aviso('Custos salvos. Os preços das receitas já foram atualizados.');
      await queryClient.invalidateQueries();
    } catch (e) {
      setErro(e);
    } finally {
      setEnviando(false);
    }
  }

  const nomes = ['salarioDesejadoMes', 'horasTrabalhadasMes', 'gasPrecoBotijao', 'gasDuracaoHoras', 'bandeiraVigente', 'criterioRateio'];
  return (
    <form className="campos" noValidate onSubmit={enviar}>
      <ProblemaAlerta erro={erro} camposNaTela={nomes} />
      <div className="grade">
        <Campo rotulo="Quanto quer ganhar por mês (R$)" erro={erroDe(erro, 'salarioDesejadoMes')}>
          <input name="salarioDesejadoMes" type="number" min={0} step="0.01" inputMode="decimal" defaultValue={txt(config.salarioDesejadoMes)} />
        </Campo>
        <Campo rotulo="Horas de trabalho por mês" dica="8 h por dia, 20 dias = 160 h." erro={erroDe(erro, 'horasTrabalhadasMes')}>
          <input name="horasTrabalhadasMes" type="number" min={1} step="0.5" inputMode="decimal" defaultValue={txt(config.horasTrabalhadasMes)} />
        </Campo>
      </div>
      <p>Valor da sua hora: <strong className="num">{brl(valorHora)}</strong></p>
      <div className="grade">
        <Campo rotulo="Preço do botijão de gás (R$)" erro={erroDe(erro, 'gasPrecoBotijao')}>
          <input name="gasPrecoBotijao" type="number" min={0} step="0.01" inputMode="decimal" placeholder="Deixe vazio se não usa" defaultValue={txt(config.gasPrecoBotijao)} />
        </Campo>
        <Campo rotulo="Horas de forno por botijão" erro={erroDe(erro, 'gasDuracaoHoras')}>
          <input name="gasDuracaoHoras" type="number" min={0} step="0.5" inputMode="decimal" defaultValue={txt(config.gasDuracaoHoras)} />
        </Campo>
      </div>
      <div className="grade">
        <Campo rotulo="Bandeira de energia deste mês" dica="Aparece na conta de luz.">
          <select name="bandeiraVigente" defaultValue={config.bandeiraVigente}>
            {BANDEIRAS.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
          </select>
        </Campo>
        <Campo rotulo="Como dividir os custos fixos">
          <select name="criterioRateio" defaultValue={config.criterioRateio}>
            <option value="HORAS">Pelas horas de trabalho de cada receita</option>
            <option value="VENDAS">Pelas vendas estimadas no mês</option>
          </select>
        </Campo>
      </div>
      <label className="check">
        <input type="checkbox" name="permitirSaldoNegativo" defaultChecked={config.permitirSaldoNegativo} />
        Permitir produzir mesmo sem estoque suficiente (o saldo fica negativo)
      </label>
      <div><button type="submit" className="primario" disabled={enviando}>Salvar custos</button></div>
    </form>
  );
}

/** Deletes a history entry after confirmation. */
function useRemocao() {
  const [alvo, setAlvo] = useState<{ caminho: string; oQue: string } | null>(null);
  const aviso = useAviso();
  const queryClient = useQueryClient();
  const dialogo = alvo ? (
    <Confirmar titulo={`Excluir ${alvo.oQue}?`} texto="O histórico serve para calcular preços em datas diferentes. Exclua só se foi cadastrado errado."
      onFechar={() => setAlvo(null)}
      onConfirmar={async () => {
        const { caminho } = alvo;
        setAlvo(null);
        try { await del(caminho); aviso('Excluído.'); await queryClient.invalidateQueries(); } catch (e) { aviso((e as Error).message, 'erro'); }
      }} />
  ) : null;
  return { pedir: (caminho: string, oQue: string) => setAlvo({ caminho, oQue }), dialogo };
}

function Tarifas() {
  const q = useQuery({
    queryKey: ['tarifas'],
    queryFn: async () => {
      const [lista, vigente] = await Promise.all([
        get<Pagina<Tarifa>>('/configuracao/tarifas?size=50'), talvez(get<Tarifa>('/configuracao/tarifas/vigente')),
      ]);
      return { lista, vigente };
    },
  });
  const [erro, setErro] = useState<unknown>(null);
  const aviso = useAviso();
  const queryClient = useQueryClient();
  const remocao = useRemocao();

  async function enviar(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    const form = ev.currentTarget;
    const f = Object.fromEntries(new FormData(form)) as Record<string, string>;
    try {
      await post('/configuracao/tarifas', { tarifaKwh: numero(f.tarifaKwh), vigenciaInicio: f.vigenciaInicio || null });
      setErro(null);
      form.reset();
      aviso('Tarifa adicionada.');
      await queryClient.invalidateQueries();
    } catch (e) { setErro(e); }
  }

  return (
    <section className="painel">
      <h2>Tarifa de energia</h2>
      {q.isPending ? <Carregando /> : q.isError ? <ErroCarregar erro={q.error} /> : (
        <>
          <p className="muted">
            {q.data.vigente
              ? <>Vigente hoje: <strong className="num">{reais6(q.data.vigente.tarifaKwh)}</strong> por kWh, desde {data(q.data.vigente.vigenciaInicio)}.</>
              : 'Nenhuma tarifa vigente. Receitas com equipamento elétrico não terão preço até você cadastrar uma.'}
          </p>
          <form className="filtros" noValidate onSubmit={enviar}>
            <Campo rotulo="Tarifa por kWh (R$)" erro={erroDe(erro, 'tarifaKwh')}>
              <input name="tarifaKwh" type="number" min={0} step="0.000001" inputMode="decimal" placeholder="Ex.: 0,80" />
            </Campo>
            <Campo rotulo="Vale a partir de" erro={erroDe(erro, 'vigenciaInicio')}>
              <input name="vigenciaInicio" type="date" defaultValue={hoje()} />
            </Campo>
            <button type="submit" className="primario">Adicionar tarifa</button>
          </form>
          <ProblemaAlerta erro={erro} camposNaTela={['tarifaKwh', 'vigenciaInicio']} />
          {q.data.lista.content.length ? (
            <ul className="lista-simples">
              {q.data.lista.content.map((t) => (
                <li key={t.id}>
                  <span>Desde {data(t.vigenciaInicio)}</span>
                  <span><span className="num">{reais6(t.tarifaKwh)}/kWh </span>
                    <button type="button" className="perigo" onClick={() => remocao.pedir(`/configuracao/tarifas/${t.id}`, 'esta tarifa')}>Excluir</button></span>
                </li>
              ))}
            </ul>
          ) : null}
        </>
      )}
      {remocao.dialogo}
    </section>
  );
}

function Bandeiras() {
  const q = useQuery({
    queryKey: ['bandeiras'],
    queryFn: async () => {
      const [lista, vigentes] = await Promise.all([
        get<Pagina<AdicionalBandeira>>('/configuracao/bandeiras?size=50'), get<AdicionalBandeira[]>('/configuracao/bandeiras/vigentes'),
      ]);
      return { lista, vigentes };
    },
  });
  const [erro, setErro] = useState<unknown>(null);
  const aviso = useAviso();
  const queryClient = useQueryClient();
  const remocao = useRemocao();

  async function enviar(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    const form = ev.currentTarget;
    const f = Object.fromEntries(new FormData(form)) as Record<string, string>;
    try {
      await post('/configuracao/bandeiras', { bandeira: f.bandeira, adicionalKwh: numero(f.adicionalKwh), vigenciaInicio: f.vigenciaInicio || null });
      setErro(null);
      form.reset();
      aviso('Valor da bandeira adicionado.');
      await queryClient.invalidateQueries();
    } catch (e) { setErro(e); }
  }

  return (
    <section className="painel">
      <h2>Adicional das bandeiras</h2>
      <p className="muted">Valor extra por kWh cobrado em cada bandeira. A verde sem valor cadastrado conta como zero.</p>
      {q.isPending ? <Carregando /> : q.isError ? <ErroCarregar erro={q.error} /> : (
        <>
          {q.data.vigentes.length ? (
            <p>Vigentes hoje: {q.data.vigentes.map((v, i) => (
              <span key={v.id}>{i ? ', ' : ''}<strong>{nomeBandeira(v.bandeira)}</strong> {reais6(v.adicionalKwh)}</span>
            ))}</p>
          ) : null}
          <form className="filtros" noValidate onSubmit={enviar}>
            <Campo rotulo="Bandeira">
              <select name="bandeira" defaultValue="AMARELA">
                {BANDEIRAS.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
              </select>
            </Campo>
            <Campo rotulo="Adicional por kWh (R$)" erro={erroDe(erro, 'adicionalKwh')}>
              <input name="adicionalKwh" type="number" min={0} step="0.000001" inputMode="decimal" placeholder="Ex.: 0,01885" />
            </Campo>
            <Campo rotulo="Vale a partir de" erro={erroDe(erro, 'vigenciaInicio')}>
              <input name="vigenciaInicio" type="date" defaultValue={hoje()} />
            </Campo>
            <button type="submit" className="primario">Adicionar valor</button>
          </form>
          <ProblemaAlerta erro={erro} camposNaTela={['adicionalKwh', 'vigenciaInicio', 'bandeira']} />
          {q.data.lista.content.length ? (
            <ul className="lista-simples">
              {q.data.lista.content.map((a) => (
                <li key={a.id}>
                  <span>{nomeBandeira(a.bandeira)}, desde {data(a.vigenciaInicio)}</span>
                  <span><span className="num">{reais6(a.adicionalKwh)}/kWh </span>
                    <button type="button" className="perigo" onClick={() => remocao.pedir(`/configuracao/bandeiras/${a.id}`, 'este valor')}>Excluir</button></span>
                </li>
              ))}
            </ul>
          ) : null}
        </>
      )}
      {remocao.dialogo}
    </section>
  );
}
