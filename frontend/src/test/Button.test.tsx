import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';

describe('UI Primitives', () => {
  it('renders button with correct text and handles disabled state', () => {
    render(<Button disabled>Submit Action</Button>);
    const button = screen.getByRole('button', { name: /submit action/i });
    expect(button).toBeDisabled();
  });

  it('renders badge with appropriate variant styling', () => {
    render(<Badge variant="destructive">CRITICAL</Badge>);
    expect(screen.getByText('CRITICAL')).toBeInTheDocument();
  });
});
