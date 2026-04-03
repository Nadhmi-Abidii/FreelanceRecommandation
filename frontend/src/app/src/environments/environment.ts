function resolveApiUrl(): string {
  const browserLocation = globalThis.location;
  const protocol = browserLocation?.protocol?.startsWith('http') ? browserLocation.protocol : 'http:';
  const hostname = browserLocation?.hostname || 'localhost';

  return `${protocol}//${hostname}:9020`;
}

export const environment = {
  production: false,
  apiUrl: resolveApiUrl()
};
