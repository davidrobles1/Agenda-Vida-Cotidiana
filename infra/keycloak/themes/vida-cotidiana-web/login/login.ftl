<#-- =========================================================
     VIDA COTIDIANA — LOGIN
     Migración de LoginPage.tsx a Keycloak/FreeMarker

     Estructura visual:
       Brand
       Bienvenido
       Inicia sesión para continuar
       Formulario
       Iniciar sesión
       Crear una cuenta
       Footer

     Se conserva:
       - Authorization Code + PKCE
       - loginAction de Keycloak
       - rememberMe
       - forgot password
       - password visibility
       - errores de usuario/password
       - credentialId
       - registrationUrl
       - social providers
     ========================================================= -->

<#import "template.ftl" as layout>

<@layout.registrationLayout
    displayMessage=!messagesPerField.existsError('username','password')
    displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??
    ; section
>

    <#if section = "header">

        <!-- =================================================
             BRAND
             ================================================= -->

        <div class="vc-brand" aria-hidden="true">

            <svg
                class="vc-brand-icon"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
            >
                <path
                    d="M12 21C12 21 5 16.5 5 10.5C5 6.91 7.69 4 11 4C11.55 4 12.09 4.08 12.6 4.23C13.32 2.9 14.69 2 16.28 2C18.65 2 20.5 3.98 20.5 6.35C20.5 12.65 12 21 12 21Z"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                />

                <path
                    d="M12 20C12 15.5 13.5 11.5 17 8"
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


        <!-- =================================================
             WELCOME
             Equivalente al bloque .welcome del TSX
             ================================================= -->

        <div class="vc-welcome">

            <h2 class="vc-welcome-title">
                Bienvenido
            </h2>

            <p class="vc-welcome-text">
                Inicia sesión para continuar
            </p>

        </div>


        <!--
            El h1 real de Keycloak permanece presente para
            accesibilidad, pero visualmente se oculta porque
            el copy visible ya pertenece al diseño de Vida
            Cotidiana.
        -->
        <span class="vc-accessible-title">
            ${msg("loginAccountTitle")}
        </span>


    <#elseif section = "form">

        <!-- =================================================
             LOGIN FORM
             ================================================= -->

        <div id="kc-form">
            <div id="kc-form-wrapper">

                <#if realm.password>

                    <form
                        id="kc-form-login"
                        class="${properties.kcFormClass!}"
                        onsubmit="login.disabled = true; return true;"
                        action="${url.loginAction}"
                        method="post"
                    >

                        <!-- =====================================
                             USERNAME
                             ===================================== -->

                        <#if !usernameHidden??>

                            <div class="vc-form-group">

                                <label
                                    for="username"
                                    class="vc-form-label"
                                >
                                    <#if !realm.loginWithEmailAllowed>
                                        ${msg("username")}
                                    <#elseif !realm.registrationEmailAsUsername>
                                        ${msg("usernameOrEmail")}
                                    <#else>
                                        ${msg("email")}
                                    </#if>
                                </label>


                                <div
                                    class="vc-input-wrapper <#if messagesPerField.existsError('username','password')>vc-input-error</#if>"
                                >

                                    <input
                                        tabindex="1"
                                        id="username"
                                        name="username"
                                        value="${(login.username!'')}"
                                        type="text"
                                        autofocus
                                        autocomplete="username"
                                        aria-invalid="<#if messagesPerField.existsError('username','password')>true<#else>false</#if>"
                                    />

                                    <#if messagesPerField.existsError('username','password')>

                                        <span
                                            class="vc-input-error-icon"
                                            aria-hidden="true"
                                        >
                                            !
                                        </span>

                                    </#if>

                                </div>


                                <#if messagesPerField.existsError('username','password')>

                                    <span
                                        id="input-error"
                                        class="vc-input-error-message"
                                        aria-live="polite"
                                    >
                                        ${kcSanitize(
                                            messagesPerField.getFirstError('username','password')
                                        )?no_esc}
                                    </span>

                                </#if>

                            </div>

                        </#if>


                        <!-- =====================================
                             PASSWORD
                             ===================================== -->

                        <div class="vc-form-group">

                            <label
                                for="password"
                                class="vc-form-label"
                            >
                                ${msg("password")}
                            </label>


                            <div class="vc-password-wrapper">

                                <div class="vc-input-wrapper">

                                    <input
                                        tabindex="2"
                                        id="password"
                                        name="password"
                                        type="password"
                                        autocomplete="current-password"
                                        aria-invalid="<#if messagesPerField.existsError('username','password')>true<#else>false</#if>"
                                    />

                                </div>


                                <!--
                                    Conservamos el mecanismo oficial
                                    de Keycloak passwordVisibility.js.
                                -->
                                <button
                                    class="${properties.kcFormPasswordVisibilityButtonClass!} vc-password-toggle"
                                    type="button"
                                    aria-label="${msg('showPassword')}"
                                    aria-controls="password"
                                    data-password-toggle
                                    data-icon-show="${properties.kcFormPasswordVisibilityIconShow!}"
                                    data-icon-hide="${properties.kcFormPasswordVisibilityIconHide!}"
                                    data-label-show="${msg('showPassword')}"
                                    data-label-hide="${msg('hidePassword')}"
                                >
                                    <i
                                        class="${properties.kcFormPasswordVisibilityIconShow!}"
                                        aria-hidden="true"
                                    ></i>
                                </button>

                            </div>


                            <#if usernameHidden?? && messagesPerField.existsError('username','password')>

                                <span
                                    id="input-error"
                                    class="vc-input-error-message"
                                    aria-live="polite"
                                >
                                    ${kcSanitize(
                                        messagesPerField.getFirstError('username','password')
                                    )?no_esc}
                                </span>

                            </#if>

                        </div>


                        <!-- =====================================
                             REMEMBER / FORGOT
                             ===================================== -->

                        <div class="vc-form-options">

                            <#if realm.rememberMe && !usernameHidden??>

                                <label class="vc-remember">

                                    <#if login.rememberMe??>

                                        <input
                                            tabindex="3"
                                            id="rememberMe"
                                            name="rememberMe"
                                            type="checkbox"
                                            checked
                                        />

                                    <#else>

                                        <input
                                            tabindex="3"
                                            id="rememberMe"
                                            name="rememberMe"
                                            type="checkbox"
                                        />

                                    </#if>

                                    <span>
                                        ${msg("rememberMe")}
                                    </span>

                                </label>

                            </#if>


                            <#if realm.resetPasswordAllowed>

                                <a
                                    tabindex="5"
                                    class="vc-forgot-password"
                                    href="${url.loginResetCredentialsUrl}"
                                >
                                    ${msg("doForgotPassword")}
                                </a>

                            </#if>

                        </div>


                        <!-- =====================================
                             BOTÓN PRINCIPAL
                             Equivalente a:

                             <IconUser />
                             Iniciar sesión
                             <IconArrowRight />
                             ===================================== -->

                        <div id="kc-form-buttons" class="vc-actions">

                            <input
                                type="hidden"
                                id="id-hidden-input"
                                name="credentialId"
                                <#if auth.selectedCredential?has_content>
                                    value="${auth.selectedCredential}"
                                </#if>
                            />


                            <button
                                tabindex="4"
                                class="vc-login-button"
                                name="login"
                                id="kc-login"
                                type="submit"
                            >

                                <!-- User icon -->

                                <svg
                                    class="vc-button-icon"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    xmlns="http://www.w3.org/2000/svg"
                                    aria-hidden="true"
                                >
                                    <circle
                                        cx="12"
                                        cy="8"
                                        r="3.5"
                                        stroke="currentColor"
                                        stroke-width="1.7"
                                    />

                                    <path
                                        d="M5.5 20C6.3 16.6 8.4 14.5 12 14.5C15.6 14.5 17.7 16.6 18.5 20"
                                        stroke="currentColor"
                                        stroke-width="1.7"
                                        stroke-linecap="round"
                                    />
                                </svg>


                                <span>
                                    Iniciar sesión
                                </span>


                                <!-- Arrow icon -->

                                <svg
                                    class="vc-button-icon"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    xmlns="http://www.w3.org/2000/svg"
                                    aria-hidden="true"
                                >
                                    <path
                                        d="M5 12H18"
                                        stroke="currentColor"
                                        stroke-width="1.8"
                                        stroke-linecap="round"
                                    />

                                    <path
                                        d="M13 7L18 12L13 17"
                                        stroke="currentColor"
                                        stroke-width="1.8"
                                        stroke-linecap="round"
                                        stroke-linejoin="round"
                                    />
                                </svg>

                            </button>

                        </div>

                    </form>

                </#if>


                <!-- =================================================
                     FOOTER TAGLINE
                     ================================================= -->

                <div class="vc-form-footer" aria-hidden="true">

                    <svg
                        class="vc-footer-icon"
                        viewBox="0 0 24 24"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                    >
                        <rect
                            x="5"
                            y="10"
                            width="14"
                            height="10"
                            rx="2"
                            stroke="currentColor"
                            stroke-width="1.6"
                        />

                        <path
                            d="M8 10V7C8 4.79 9.79 3 12 3C14.21 3 16 4.79 16 7V10"
                            stroke="currentColor"
                            stroke-width="1.6"
                            stroke-linecap="round"
                        />

                    </svg>

                    <span>
                        Tu hogar, tus momentos, tu agenda.
                    </span>

                </div>

            </div>
        </div>


        <script
            type="module"
            src="${url.resourcesPath}/js/passwordVisibility.js"
        ></script>


    <#elseif section = "info">

        <!-- =================================================
             REGISTER
             ================================================= -->

        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>

            <div
                id="kc-registration-container"
                class="pf-v5-c-login__main-footer-band"
            >

                <div
                    id="kc-registration"
                    class="pf-v5-c-login__main-footer-band-item"
                >

                    <a
                        tabindex="6"
                        href="${url.registrationUrl}"
                        class="vc-register-button"
                    >

                        <svg
                            class="vc-button-icon"
                            viewBox="0 0 24 24"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg"
                            aria-hidden="true"
                        >
                            <circle
                                cx="9"
                                cy="8"
                                r="3.5"
                                stroke="currentColor"
                                stroke-width="1.7"
                            />

                            <path
                                d="M3.5 20C4.2 16.6 6.1 14.5 9 14.5C11.9 14.5 13.8 16.6 14.5 20"
                                stroke="currentColor"
                                stroke-width="1.7"
                                stroke-linecap="round"
                            />

                            <path
                                d="M18 8V15"
                                stroke="currentColor"
                                stroke-width="1.7"
                                stroke-linecap="round"
                            />

                            <path
                                d="M14.5 11.5H21.5"
                                stroke="currentColor"
                                stroke-width="1.7"
                                stroke-linecap="round"
                            />
                        </svg>

                        <span>
                            Crear una cuenta
                        </span>

                        <svg
                            class="vc-button-icon"
                            viewBox="0 0 24 24"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg"
                            aria-hidden="true"
                        >
                            <path
                                d="M5 12H18"
                                stroke="currentColor"
                                stroke-width="1.8"
                                stroke-linecap="round"
                            />

                            <path
                                d="M13 7L18 12L13 17"
                                stroke="currentColor"
                                stroke-width="1.8"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                            />
                        </svg>

                    </a>

                </div>

            </div>

        </#if>


    <#elseif section = "socialProviders">

        <!-- =================================================
             SOCIAL PROVIDERS
             ================================================= -->

        <#if realm.password && social.providers??>

            <div
                id="kc-social-providers"
                class="${properties.kcFormSocialAccountSectionClass!}"
            >

                <ul
                    class="${properties.kcFormSocialAccountListClass!}
                    <#if social.providers?size gt 3>
                        ${properties.kcFormSocialAccountListGridClass!}
                    </#if>"
                >

                    <#list social.providers as p>

                        <li
                            class="${properties.kcFormSocialAccountListItemClass!}"
                        >

                            <a
                                id="social-${p.alias}"
                                class="${properties.kcFormSocialAccountListButtonClass!}
                                <#if social.providers?size gt 3>
                                    ${properties.kcFormSocialAccountGridItem!}
                                </#if>"
                                aria-label="${p.displayName}"
                                type="button"
                                href="${p.loginUrl}"
                            >

                                <span
                                    class="${properties.kcFormSocialAccountNameClass!}"
                                >
                                    ${p.displayName!}
                                </span>

                            </a>

                        </li>

                    </#list>

                </ul>

            </div>

        </#if>

    </#if>

</@layout.registrationLayout>