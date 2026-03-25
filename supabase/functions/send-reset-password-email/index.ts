/// <reference types="https://esm.sh/@supabase/functions-js@2" />
import { corsHeaders } from '../../_shared/cors.ts'

interface ResetPasswordPayload {
  email: string;
  resetLink: string;
  userName?: string;
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    const payload: ResetPasswordPayload = await req.json();
    const { email, resetLink, userName } = payload;

    if (!email || !resetLink) {
      return new Response(
        JSON.stringify({ error: 'Campos obrigatórios ausentes: email e resetLink.' }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
      );
    }

    const RESEND_API_KEY = Deno.env.get('RESEND_API_KEY');
    if (!RESEND_API_KEY) {
      throw new Error('RESEND_API_KEY não encontrada nas variáveis de ambiente.');
    }

    const displayName = userName ?? email;

    const htmlBody = `
      <!DOCTYPE html>
      <html lang="pt-BR">
      <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <title>Redefinição de Senha</title>
        <style>
          body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
          .container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
          .header { background-color: #1a1a2e; padding: 32px; text-align: center; }
          .header h1 { color: #ffffff; margin: 0; font-size: 24px; }
          .body { padding: 32px; color: #333333; }
          .body p { line-height: 1.6; margin: 0 0 16px; }
          .btn-container { text-align: center; margin: 32px 0; }
          .btn { display: inline-block; background-color: #e94560; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 6px; font-size: 16px; font-weight: bold; }
          .footer { background-color: #f4f4f4; padding: 16px 32px; text-align: center; font-size: 12px; color: #888888; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <h1>World Jackpot</h1>
          </div>
          <div class="body">
            <p>Olá, <strong>${displayName}</strong>!</p>
            <p>Recebemos uma solicitação para redefinir a senha da sua conta. Clique no botão abaixo para criar uma nova senha:</p>
            <div class="btn-container">
              <a href="${resetLink}" class="btn">Redefinir minha senha</a>
            </div>
            <p>Se você não solicitou a redefinição de senha, ignore este email. Sua senha permanecerá a mesma.</p>
            <p>Este link é válido por <strong>1 hora</strong>.</p>
          </div>
          <div class="footer">
            <p>© ${new Date().getFullYear()} World Jackpot. Todos os direitos reservados.</p>
            <p>Se o botão não funcionar, copie e cole este link no seu navegador:<br/>${resetLink}</p>
          </div>
        </div>
      </body>
      </html>
    `;

    const resendResponse = await fetch('https://api.resend.com/emails', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${RESEND_API_KEY}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        from: 'World Jackpot <noreply@worldjackpot.com>',
        to: [email],
        subject: 'Redefinição de senha - World Jackpot',
        html: htmlBody,
      }),
    });

    const resendData = await resendResponse.json();

    if (!resendResponse.ok) {
      console.error('[send-reset-password-email] Erro ao enviar email via Resend:', resendData);
      throw new Error(`Resend API error: ${JSON.stringify(resendData)}`);
    }

    console.log(`[send-reset-password-email] Email enviado com sucesso para ${email}. ID: ${resendData.id}`);

    return new Response(
      JSON.stringify({ success: true, message: 'Email de redefinição de senha enviado com sucesso.' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
    );

  } catch (err) {
    console.error('[send-reset-password-email] ERRO:', err);
    return new Response(
      JSON.stringify({ error: err?.message ?? 'Erro interno no servidor.' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
    );
  }
});
