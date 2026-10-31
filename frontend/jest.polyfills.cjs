// React Router needs these, and jsdom does not provide them.
const util = require('node:util');

Object.assign(globalThis, { TextDecoder: util.TextDecoder, TextEncoder: util.TextEncoder });
