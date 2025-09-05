import { render, screen } from '@testing-library/react';
import App from './App';

test('renders login page by default', async () => {
  render(<App />);
  const loginTexts = await screen.findAllByText(/Log in/i);
  expect(loginTexts.length).toBeGreaterThan(0);
});
