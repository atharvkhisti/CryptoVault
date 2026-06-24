import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../services/authApi';
import { useToast } from '../context/ToastContext';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { Logo } from '../components/ui/Logo';
import { Mail, Eye, EyeOff } from 'lucide-react';
import { motion } from 'framer-motion';

const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters long'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export const Login: React.FC = () => {
  const { login } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (values: LoginFormValues) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(values.email, values.password);
      if (response.success && response.data) {
        localStorage.setItem('cryptovault_token', response.data.accessToken);

        // Fetch user profile immediately to fill context state
        const profileRes = await authApi.getProfile();
        if (profileRes.success && profileRes.data) {
          login(response.data.accessToken, profileRes.data);
          toast({
            title: 'Welcome Back',
            description: `Successfully signed in as ${profileRes.data.name}`,
            variant: 'success',
          });
          navigate('/');
        } else {
          toast({
            title: 'Login Error',
            description: 'Could not resolve user details.',
            variant: 'error',
          });
        }
      } else {
        toast({
          title: 'Authentication Failed',
          description: response.message || 'Check email or password.',
          variant: 'error',
        });
      }
    } catch (error: any) {
      console.error(error);
      const errMsg = error.response?.data?.message || 'Authentication error. Please verify credentials.';
      toast({
        title: 'Authentication Failed',
        description: errMsg,
        variant: 'error',
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#020204] text-foreground flex items-center justify-center p-4 relative overflow-hidden font-sans">
      {/* Background Radial Glow */}
      <div className="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[550px] h-[550px] bg-gradient-to-tr from-primary/10 to-secondary/5 rounded-full blur-[100px] pointer-events-none -z-10" />

      <motion.div
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="w-full max-w-md"
      >
        <Card className="border-zinc-800 bg-[#09090B]/90 backdrop-blur-2xl rounded-[32px] overflow-hidden shadow-2xl relative">
          <CardHeader className="text-center pt-8">
            {/* Brand logo mark */}
            <div className="mx-auto w-16 h-16 rounded-2xl bg-gradient-to-br from-violet-950/80 to-indigo-950/80 border border-violet-800/40 shadow-2xl shadow-violet-900/30 flex items-center justify-center mb-4">
              <Logo size={44} />
            </div>
            <div className="mb-1">
              <span className="font-black text-lg tracking-tight text-white">
                Crypto<span className="text-violet-400">Vault</span>
              </span>
            </div>
            <CardTitle className="text-2xl font-bold tracking-tight text-zinc-100">Sign In</CardTitle>
            <CardDescription className="text-zinc-400 mt-1">
              Connect to your CryptoVault secure wallet dashboard
            </CardDescription>
          </CardHeader>

          <CardContent className="px-6 pb-6 pt-2">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div className="relative">
                <Mail className="absolute right-4 top-[42px] text-zinc-500" size={16} />
                <Input
                  label="Email Address"
                  placeholder="you@domain.com"
                  type="email"
                  error={errors.email?.message}
                  {...register('email')}
                  className="pr-12"
                />
              </div>

              <div className="relative">
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-4 top-[42px] text-zinc-500 hover:text-white transition-colors cursor-pointer"
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
                <Input
                  label="Password"
                  placeholder="••••••••"
                  type={showPassword ? 'text' : 'password'}
                  error={errors.password?.message}
                  {...register('password')}
                  className="pr-12"
                />
              </div>

              <Button type="submit" className="w-full mt-4 h-12 text-sm bg-gradient-to-r from-primary to-secondary hover:opacity-90 shadow-md shadow-primary/25 border-0 rounded-full text-white font-semibold" isLoading={isLoading}>
                Open Wallet
              </Button>
            </form>
          </CardContent>

          <div className="px-6 py-5 border-t border-zinc-900 bg-zinc-950/40 text-center text-xs text-zinc-400">
            Need a secure wallet?{' '}
            <Link to="/register" className="text-primary hover:text-purple-400 font-bold transition">
              Create account
            </Link>
          </div>
        </Card>
      </motion.div>
    </div>
  );
};
export default Login;
