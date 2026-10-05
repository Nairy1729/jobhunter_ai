import { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('JobHunter Uncaught UI Error:', error, errorInfo);
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-dark-950 flex flex-col items-center justify-center p-4 text-ink-primary font-sans">
          <div className="w-full max-w-md p-8 rounded-2xl bg-dark-900 border border-white/[0.08] shadow-2xl text-center space-y-5">
            <div className="w-12 h-12 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 mx-auto flex items-center justify-center">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <div className="space-y-2">
              <h2 className="text-xl font-light text-ink-primary">
                Application Rendering Interrupted
              </h2>
              <p className="text-xs text-ink-secondary leading-relaxed font-light">
                JobHunter encountered an unexpected rendering issue. Your master facts and session remain intact.
              </p>
            </div>

            {this.state.error?.message && (
              <div className="p-3 rounded-lg bg-dark-950 border border-white/[0.04] text-[11px] font-mono text-ink-muted text-left overflow-x-auto max-h-24">
                {this.state.error.message}
              </div>
            )}

            <div className="flex items-center justify-center gap-3 pt-2">
              <button
                onClick={this.handleReset}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-signal-emerald text-dark-950 font-mono text-xs font-bold hover:bg-emerald-400 transition"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>RELOAD APPLICATION</span>
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
