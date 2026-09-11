export type AccessTokenProvider = () => string | null | Promise<string | null>

let accessTokenProvider: AccessTokenProvider | null = null
let csrfCookieName = 'XSRF-TOKEN'
let csrfHeaderName = 'X-XSRF-TOKEN'

/** Installs the hospital/IdP-specific token adapter without coupling coreIb to one OIDC SDK. */
export function configureAccessTokenProvider(provider: AccessTokenProvider | null) {
  accessTokenProvider = provider
}

export function configureCsrfProtection(cookieName?: string | null, headerName?: string | null) {
  if (cookieName) csrfCookieName = cookieName
  if (headerName) csrfHeaderName = headerName
}

export async function coreIbFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers)
  if (accessTokenProvider && !headers.has('Authorization')) {
    const token = await accessTokenProvider()
    if (token) headers.set('Authorization', `Bearer ${token}`)
  }

  const method = (init.method ?? 'GET').toUpperCase()
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method) && !headers.has(csrfHeaderName)) {
    const csrfToken = readCookie(csrfCookieName)
    if (csrfToken) headers.set(csrfHeaderName, csrfToken)
  }

  return fetch(input, { ...init, headers, credentials: init.credentials ?? 'same-origin' })
}

function readCookie(name: string): string | null {
  if (typeof document === 'undefined') return null
  const prefix = `${encodeURIComponent(name)}=`
  const cookie = document.cookie.split('; ').find((entry) => entry.startsWith(prefix))
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null
}
