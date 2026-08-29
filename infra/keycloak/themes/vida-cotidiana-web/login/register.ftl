<#-- UX-006/UX-008/Documentacion/02-ux-ui/login-theme.md: copy of
     keycloak.v2's real register.ftl (extracted from the running
     quay.io/keycloak/keycloak:25.0 image's
     org.keycloak.keycloak-themes-25.0.6.jar), same minimal-diff discipline
     already used for login.ftl — exactly one addition, the "vc-brand" div
     (restyled 2026-08-17 to match login.ftl/LoginPage.tsx, see login.css),
     placed in the header section the same way. Everything else (imports,
     form fields, user-profile-driven fields, password/password-confirm,
     terms acceptance, submit) is untouched, so the real registration flow
     behaves identically to the stock theme. -->
<#import "template.ftl" as layout>
<#import "user-profile-commons.ftl" as userProfileCommons>
<#import "register-commons.ftl" as registerCommons>
<@layout.registrationLayout displayMessage=messagesPerField.exists('global') displayRequiredFields=true; section>
    <#if section = "header">
        <#-- UX-016: mismo bloque de marca que login.ftl (icono incluido) para
             que "Iniciar sesión" y "Crear usuario" se lean como la misma
             aplicación, que es lo que pidió el usuario al reducir de 3
             pantallas a 2. Puramente decorativo. -->
        <div class="vc-brand" aria-hidden="true">
            <svg
                class="vc-brand-icon"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
            >
                <path
                    d="M11 20c0-6 3-10 9-11 0 6-3 10-9 11Z"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linejoin="round"
                />
                <path
                    d="M11 20c-4 0-7-3-7-7 0-1 .2-2 .6-2.8"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                />
            </svg>
            <div class="vc-brand-text">
                <span class="vc-brand-title">Agenda</span>
                <span class="vc-brand-script">vida Cotidiana</span>
            </div>
        </div>
        <#if messageHeader??>
            ${kcSanitize(msg("${messageHeader}"))?no_esc}
        <#else>
            ${msg("registerTitle")}
        </#if>
    <#elseif section = "form">
        <form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">

            <@userProfileCommons.userProfileFormFields; callback, attribute>
                <#if callback = "afterField">
                <#-- render password fields just under the username or email (if used as username) -->
                    <#if passwordRequired?? && (attribute.name == 'username' || (attribute.name == 'email' && realm.registrationEmailAsUsername))>
                        <div class="${properties.kcFormGroupClass!}">
                            <label for="password" class="${properties.kcLabelClass!}">
                                <span class="pf-v5-c-form__label-text">
                                    ${msg("password")}
                                    <span class="pf-v5-c-form__label-required" aria-hidden="true">&#42;</span>
                                </span>
                            </label>
                            <span class="${properties.kcInputGroup!}">
                                <span class="${properties.kcInputClass!}">
                                    <input type="password" id="password" name="password"
                                            autocomplete="new-password"
                                            aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
                                    />
                                </span>
                                <button class="${properties.kcFormPasswordVisibilityButtonClass!}" type="button" aria-label="${msg('showPassword')}"
                                        aria-controls="password" data-password-toggle
                                        data-icon-show="${properties.kcFormPasswordVisibilityIconShow!}" data-icon-hide="${properties.kcFormPasswordVisibilityIconHide!}"
                                        data-label-show="${msg('showPassword')}" data-label-hide="${msg('hidePassword')}">
                                    <i class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
                                </button>
                            </span>

                            <#if messagesPerField.existsError('password')>
                                <span id="input-error-password" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('password'))?no_esc}
                                </span>
                            </#if>
                        </div>

                        <div class="${properties.kcFormGroupClass!}">
                            <div class="${properties.kcLabelWrapperClass!}">
                                <label for="password-confirm" class="${properties.kcLabelClass!}">
                                    <span class="pf-v5-c-form__label-text">
                                        ${msg("passwordConfirm")}
                                        <span class="pf-v5-c-form__label-required" aria-hidden="true">&#42;</span>
                                    </span>
                                </label>
                            </div>
                            <div class="${properties.kcInputGroup!}">
                                <span class="${properties.kcInputClass!}">
                                    <input type="password" id="password-confirm"
                                            name="password-confirm"
                                            aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
                                    />
                                </span>
                                <button class="${properties.kcFormPasswordVisibilityButtonClass!}" type="button" aria-label="${msg('showPassword')}"
                                        aria-controls="password-confirm"  data-password-toggle
                                        data-icon-show="${properties.kcFormPasswordVisibilityIconShow!}" data-icon-hide="${properties.kcFormPasswordVisibilityIconHide!}"
                                        data-label-show="${msg('showPassword')}" data-label-hide="${msg('hidePassword')}">
                                    <i class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
                                </button>
                            </div>

                            <#if messagesPerField.existsError('password-confirm')>
                                <span id="input-error-password-confirm" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                                </span>
                            </#if>
                        </div>
                    </#if>
                </#if>
            </@userProfileCommons.userProfileFormFields>

            <@registerCommons.termsAcceptance/>

            <#if recaptchaRequired??>
                <div class="form-group">
                    <div class="${properties.kcInputWrapperClass!}">
                        <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
                    </div>
                </div>
            </#if>

            <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doRegister")}"/>
            </div>
            <div class="${properties.kcFormGroupClass!} pf-v5-c-login__main-footer-band">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!} pf-v5-c-login__main-footer-band-item">
                    <#-- UX-016: "vc-back-to-login" es solo un gancho de estilo para
                         darle a este enlace la piel del botón contorneado que el
                         portal usaba en "Crear una cuenta". El href, el mensaje y
                         el comportamiento son los de Keycloak, sin tocar. -->
                    <div class="${properties.kcFormOptionsWrapperClass!} vc-back-to-login">
                        <span><a href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
                    </div>
                </div>
            </div>

        </form>

        <#-- UX-016: mismo pie que la tarjeta de LoginPage/login.ftl. -->
        <div class="vc-form-footer" aria-hidden="true">
            <svg class="vc-footer-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="5" y="10" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.6"/>
                <path d="M8 10V7C8 4.79 9.79 3 12 3C14.21 3 16 4.79 16 7V10" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
            <span>Tu hogar, tus momentos, tu agenda.</span>
        </div>

        <script type="module" src="${url.resourcesPath}/js/passwordVisibility.js"></script>
    </#if>
</@layout.registrationLayout>
