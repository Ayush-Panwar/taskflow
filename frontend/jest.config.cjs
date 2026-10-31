// A zone west of UTC, where reading a date like 2026-03-15 as UTC would show the wrong day.
process.env.TZ = 'America/New_York';

module.exports = {
  testEnvironment: 'jsdom',
  setupFiles: ['<rootDir>/jest.polyfills.cjs'],
  setupFilesAfterEnv: ['<rootDir>/src/test/setup.ts'],
  testMatch: ['<rootDir>/src/**/*.test.{ts,tsx}'],
  // Each test sets up its own mock responses, and none can leak into the next one.
  resetMocks: true,
  // Generous, so a busy machine or CI runner does not cause false failures.
  testTimeout: 30000,
  transform: {
    '^.+\\.tsx?$': [
      '@swc/jest',
      {
        jsc: {
          parser: { syntax: 'typescript', tsx: true },
          transform: { react: { runtime: 'automatic' } },
          target: 'es2022',
        },
        module: { type: 'commonjs' },
      },
    ],
  },
};
