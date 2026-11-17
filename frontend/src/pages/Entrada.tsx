import { type FormEvent, useEffect, useRef, useState } from 'react';
import { ApiError } from '../api/client';
import { useSessao } from '../auth/sessao';
import { Fatia } from '../components/Fatia';
import { Campo, ProblemaAlerta, erroDe } from '../components/ui';

type Modo = 'entrar' | 'cadastrar';

// The demo account's small Dutch pie: half a recipe (R$ 27,50 of ingredients, R$ 5 of work) plus a R$ 1 box.
const EXEMPLO = { insumos: 27.5, embalagens: 1, maoDeObra: 5, energia: 0, gas: 0, custosFixos: 0, total: 33.5 };

export function Entrada() {
  const { entrar, cadastrar, aviso } = useSessao();
  const [modo, setModo] = useState<Modo>('entrar');
  const [erro, setErro] = useState<unknown>(null);
  const [enviando, setEnviando] = useState(false);
  const primeiro = useRef<HTMLInputElement>(null);
  const cadastro = modo === 'cadastrar';

  useEffect(() => { document.title = 'Confeitaria | Preço e estoque'; }, []);
  useEffect(() => { setErro(null); primeiro.current?.focus(); }, [modo]);

  async function enviar(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    const f = Object.fromEntries(new FormData(ev.currentTarget)) as Record<string, string>;
    setEnviando(true);
    try {
      if (cadastro) await cadastrar(f.nome, f.email, f.senha);
      else await entrar(f.email, f.senha);
    } catch (e) {
      setErro(e instanceof ApiError && e.status === 401 ? new ApiError(401, { detail: 'E-mail ou senha incorretos.' }) : e);
      setEnviando(false);
    }
  }

  const aba = (id: Modo, rotulo: string) => (
    <button type="button" role="tab" aria-selected={modo === id} onClick={() => setModo(id)}>{rotulo}</button>
  );

  return (
    <div className="entrada">
      <section className="entrada-arte">
        <h1>Quanto custa cada fatia?</h1>
        <p>Some ingredientes, embalagem, seu tempo, energia e contas do mês. Veja o preço certo para o balcão e para cada aplicativo, e acompanhe o estoque a cada produção.</p>
        <figure className="fatia-arte">
          <Fatia composicao={EXEMPLO} rotuloTotal="Custo da torta pequena" />
          <figcaption className="exemplo">Exemplo: meia receita de torta holandesa custa R$ 33,50 e sai a R$ 55,83 com 40% de margem.</figcaption>
        </figure>
      </section>
      <section className="entrada-form">
        <div className="caixa">
          <div className="abas" role="tablist">{aba('entrar', 'Entrar')}{aba('cadastrar', 'Criar conta')}</div>
          {aviso && !cadastro ? <div className="alerta aviso" role="status"><p>{aviso}</p></div> : null}
          <form className="campos" noValidate onSubmit={enviar} key={modo}>
            <ProblemaAlerta erro={erro} camposNaTela={['nome', 'email', 'senha']} />
            {cadastro ? (
              <Campo rotulo="Seu nome" erro={erroDe(erro, 'nome')}>
                <input name="nome" autoComplete="name" required maxLength={120} ref={primeiro} />
              </Campo>
            ) : null}
            <Campo rotulo="E-mail" erro={erroDe(erro, 'email')}>
              <input name="email" type="email" autoComplete="email" required ref={cadastro ? undefined : primeiro} />
            </Campo>
            <Campo rotulo="Senha" dica={cadastro ? 'Pelo menos 8 caracteres.' : undefined} erro={erroDe(erro, 'senha')}>
              <input name="senha" type="password" autoComplete={cadastro ? 'new-password' : 'current-password'} required />
            </Campo>
            <button type="submit" className="primario" disabled={enviando}>{cadastro ? 'Criar conta' : 'Entrar'}</button>
          </form>
        </div>
      </section>
    </div>
  );
}
