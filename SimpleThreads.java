public class SimpleThreads {

    // Exibe uma mensagem precedida pelo nome da thread atual
    static void threadMessage(String message) {
        String threadName = Thread.currentThread().getName();
        System.out.format("%s: %s%n", threadName, message);
    }

    private static class MessageLoop
        implements Runnable {
        public void run() {
            String importantInfo[] = {
                "Mares eat oats",
                "Does eat oats",
                "Little lambs eat ivy",
                "A kid will eat ivy too"
            };
            try {
                for (int i = 0; i < importantInfo.length; i++) {
                    // Pausa de 4 segundos
                    Thread.sleep(4000);
                    // Imprime a mensagem
                    threadMessage(importantInfo[i]);
                }
            } catch (InterruptedException e) {
                threadMessage("Eu não tinha terminado!");
            }
        }
    }

    // Tarefa CPU-intensiva: conta números primos até LIMIT usando divisão por tentativa.
    // Ao contrário do MessageLoop, nunca bloqueia em sleep(), portanto InterruptedException
    // não é lançada naturalmente — a thread precisa consultar isInterrupted() por conta
    // própria a cada iteração para permanecer interrompível.
    private static class PrimeCounter
        implements Runnable {
        private static final long LIMIT = 10_000_000L;

        public void run() {
            long count = 0;
            threadMessage("Contando primos até " + LIMIT + "...");
            for (long n = 2; n <= LIMIT; n++) {
                // Verifica antes de cada candidato para que a thread possa ser parada prontamente
                if (Thread.currentThread().isInterrupted()) {
                    threadMessage("Interrompido! Contei " + count + " primos antes do prazo.");
                    return;
                }
                if (isPrime(n)) count++;
            }
            threadMessage("Concluído! Encontrei " + count + " primos até " + LIMIT);
        }

        private boolean isPrime(long n) {
            for (long i = 2; i * i <= n; i++) {
                if (n % i == 0) return false;
            }
            return true;
        }
    }

    public static void main(String args[])
        throws InterruptedException {

        // Tempo de espera em milissegundos antes de interromper a thread MessageLoop (padrão: 1 hora)
        long patience = 1000 * 60 * 60;

        // Se um argumento for passado na linha de comando, define o patience em segundos
        if (args.length > 0) {
            try {
                patience = Long.parseLong(args[0]) * 1000;
            } catch (NumberFormatException e) {
                System.err.println("O argumento deve ser um inteiro.");
                System.exit(1);
            }
        }

        threadMessage("Iniciando a thread MessageLoop");
        long startTime = System.currentTimeMillis();
        Thread t = new Thread(new MessageLoop());

        // Inicia a thread MessageLoop
        t.start();

        // --- PrimeCounter: thread CPU-intensiva com prazo máximo de execução ---
        long cpuPatience = 5000; // prazo de 5 segundos para a tarefa CPU-intensiva
        threadMessage("Iniciando a thread PrimeCounter (prazo: " + cpuPatience / 1000 + "s)");
        long cpuStartTime = System.currentTimeMillis();
        Thread cpuThread = new Thread(new PrimeCounter(), "PrimeCounter");
        cpuThread.start();

        // Monitora o PrimeCounter e aplica o prazo, seguindo o mesmo padrão do MessageLoop abaixo
        while (cpuThread.isAlive()) {
            cpuThread.join(1000);
            if (((System.currentTimeMillis() - cpuStartTime) > cpuPatience) && cpuThread.isAlive()) {
                threadMessage("Tarefa CPU excedeu o prazo, interrompendo PrimeCounter!");
                cpuThread.interrupt();
                cpuThread.join();
            }
        }
        // -----------------------------------------------------------------------

        threadMessage("Aguardando a thread MessageLoop terminar");

        // Aguarda até que a thread MessageLoop encerre
        while (t.isAlive()) {
            threadMessage("Ainda aguardando...");
            // Espera no máximo 1 segundo para a thread MessageLoop terminar
            t.join(1000);
            if (((System.currentTimeMillis() - startTime) > patience) && t.isAlive()) {
                threadMessage("Cansado de esperar!");
                // Força a interrupção da thread MessageLoop
                t.interrupt();
                // Aguarda ela terminar — não deve demorar muito
                t.join();
            }
        }
        threadMessage("Finalmente!");
    }
}
