import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

describe('Common UI Components Test Suite', () => {
  describe('Badge Component', () => {
    it('should render default badge correctly', () => {
      render(<Badge>Active Item</Badge>);
      const badge = screen.getByText('Active Item');
      expect(badge).toBeInTheDocument();
      expect(badge.className).toContain('bg-slate-100');
    });

    it('should apply variant styles correctly', () => {
      const { rerender } = render(<Badge variant="success">Available</Badge>);
      expect(screen.getByText('Available').className).toContain('bg-emerald-50');

      rerender(<Badge variant="danger">Overdue</Badge>);
      expect(screen.getByText('Overdue').className).toContain('bg-rose-50');

      rerender(<Badge variant="warning">Held</Badge>);
      expect(screen.getByText('Held').className).toContain('bg-amber-50');
    });
  });

  describe('Modal Component', () => {
    it('should not render anything when isOpen is false', () => {
      const { container } = render(
        <Modal isOpen={false} title="Test Modal" onClose={() => {}}>
          <p>Hidden Content</p>
        </Modal>
      );
      expect(container.firstChild).toBeNull();
    });

    it('should render title and content when isOpen is true', () => {
      render(
        <Modal isOpen={true} title="Checkout Resource" onClose={() => {}}>
          <p>Modal Body Content</p>
        </Modal>
      );
      expect(screen.getByText('Checkout Resource')).toBeInTheDocument();
      expect(screen.getByText('Modal Body Content')).toBeInTheDocument();
    });

    it('should trigger onClose callback when close button is clicked', () => {
      const handleClose = vi.fn();
      render(
        <Modal isOpen={true} title="Cancel Reservation" onClose={handleClose}>
          <p>Are you sure?</p>
        </Modal>
      );
      const closeButton = screen.getByRole('button');
      fireEvent.click(closeButton);
      expect(handleClose).toHaveBeenCalledTimes(1);
    });
  });

  describe('LoadingSpinner Component', () => {
    it('should render spinner with custom loading text', () => {
      render(<LoadingSpinner text="Fetching catalog inventory..." />);
      expect(screen.getByText('Fetching catalog inventory...')).toBeInTheDocument();
    });
  });
});
