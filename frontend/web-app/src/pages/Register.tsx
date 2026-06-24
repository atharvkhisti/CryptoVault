import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../services/authApi';
import { useToast } from '../context/ToastContext';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { Logo } from '../components/ui/Logo';
import { User, Mail, Eye, EyeOff } from 'lucide-react';
import { motion } from 'framer-motion';

const registerSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters long'),
  email: z.string().email('Please enter a valid email address'),
  password: z.string()
    .min(8, 'Password must be at least 8 characters long')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
    .regex(/[0-9]/, 'Password must contain at least one digit'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export const Register: React.FC = () => {
  const { toast } = useToast();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (values: RegisterFormValues) => {
    setIsLoading(true);
    try {
      const response = await authApi.register(values.name, values.email, values.password);
      if (response.success) {
        toast({
          title: 'Account Configured',
          description: 'Wallet setup complete. You can now login.',
          variant: 'success',
        });
        navigate('/login');
      } else {
        toast({
          title: 'Setup Failed',
          description: response.message || 'Email might be already in use.',
          variant: 'error',
        });
      }
    } catch (error: any) {
      console.error(error);
      const errMsg = error.response?.data?.message || 'Error occurred during registration.';
      toast({
        title: 'Registration Error',
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
        className="w-full max-w-md animate-in fade-in zoom-in-95 duration-200"
      >
        <Card className="border-zinc-800 bg-[#09090B]/90 backdrop-blur-2xl rounded-[32px] overflow-hidden shadow-2xl relative">
          <CardHeader className="text-center pt-6">
            <div className="mx-auto w-16 h-16 rounded-2xl bg-gradient-to-br from-violet-950/80 to-indigo-950/80 border border-violet-800/40 shadow-2xl shadow-violet-900/30 flex items-center justify-center mb-4">
              <Logo size={44} />
            </div>
            <div className="mb-1">
              <span className="font-black text-lg tracking-tight text-white">
                Crypto<span className="text-violet-400">Vault</span>
              </span>
            </div>
            <CardTitle className="text-2xl font-bold tracking-tight text-zinc-100">Setup Wallet</CardTitle>
            <CardDescription className="text-zinc-400 mt-1">
              Initialize a secure new multi-chain vault portfolio
            </CardDescription>
          </CardHeader>

          <CardContent className="px-6 pb-6 pt-1">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div className="relative">
                <User className="absolute right-4 top-[42px] text-zinc-500" size={16} />
                <Input
                  label="Display Name"
                  placeholder="John Doe"
                  type="text"
                  error={errors.name?.message}
                  {...register('name')}
                  className="pr-12"
                />
              </div>

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

              <div className="relative">
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-4 top-[42px] text-zinc-500 hover:text-white transition-colors cursor-pointer"
                >
                  {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
                <Input
                  label="Confirm Password"
                  placeholder="••••••••"
                  type={showConfirmPassword ? 'text' : 'password'}
                  error={errors.confirmPassword?.message}
                  {...register('confirmPassword')}
                  className="pr-12"
                />
              </div>

              <Button type="submit" className="w-full mt-4 h-12 text-sm bg-gradient-to-r from-primary to-secondary hover:opacity-90 shadow-md shadow-primary/25 border-0 rounded-full text-white font-semibold" isLoading={isLoading}>
                Create Password
              </Button>
            </form>
          </CardContent>

          <div className="px-6 py-5 border-t border-zinc-900 bg-zinc-950/40 text-center text-xs text-zinc-400">
            Already have an account?{' '}
            <Link to="/login" className="text-primary hover:text-purple-400 font-bold transition">
              Sign in
            </Link>
          </div>
        </Card>
      </motion.div>
    </div>
  );
};
export default Register;
