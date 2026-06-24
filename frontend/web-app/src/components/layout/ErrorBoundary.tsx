import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/Card';
import { Button } from '../ui/Button';
import { AlertCircle } from 'lucide-react';

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
    console.error('Uncaught error inside component tree:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-zinc-950 flex items-center justify-center p-6">
          <Card className="max-w-md w-full border-red-900/50 bg-zinc-950/80">
            <CardHeader className="text-center">
              <div className="mx-auto w-12 h-12 rounded-full bg-red-950/30 border border-red-900/50 flex items-center justify-center mb-3">
                <AlertCircle className="text-red-500" size={24} />
              </div>
              <CardTitle className="text-red-400">Application Error</CardTitle>
              <CardDescription>
                An unexpected runtime error occurred inside this rendering container.
              </CardDescription>
            </CardHeader>
            <CardContent className="text-zinc-400 text-xs font-mono bg-zinc-900/40 p-3 rounded-md border border-zinc-800/60 break-words mb-4">
              {this.state.error?.toString() || 'Unknown application error'}
            </CardContent>
            <Button
              onClick={() => window.location.reload()}
              variant="destructive"
              className="w-full text-sm"
            >
              Reload Page
            </Button>
          </Card>
        </div>
      );
    }

    return this.props.children;
  }
}
export default ErrorBoundary;
