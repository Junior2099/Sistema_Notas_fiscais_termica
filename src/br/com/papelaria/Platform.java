package br.com.papelaria;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Abertura de arquivos e navegador em Windows e desktops Linux. */
final class Platform {
    static boolean linux(){return System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("linux");}
    static void open(Path path) throws IOException {
        Path absolute=path.toAbsolutePath();if(!Files.exists(absolute))throw new IOException("Arquivo ou pasta não encontrado: "+absolute);
        launch(absolute.toString(),false);
    }
    static void browse(URI uri) throws IOException {launch(uri.toString(),true);}
    private static void launch(String target,boolean browser) throws IOException {
        Exception cause=null;
        if(Desktop.isDesktopSupported()) {
            Desktop desktop=Desktop.getDesktop();Desktop.Action action=browser?Desktop.Action.BROWSE:Desktop.Action.OPEN;
            if(desktop.isSupported(action))try {if(browser)desktop.browse(URI.create(target));else desktop.open(new File(target));return;}
            catch(IOException|UnsupportedOperationException e){cause=e;}
        }
        if(linux())try {
            Process process=new ProcessBuilder("xdg-open",target).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if(process.waitFor(1200,TimeUnit.MILLISECONDS)&&process.exitValue()!=0)throw new IOException("Não foi possível abrir pelo aplicativo padrão do Linux.");return;
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Abertura interrompida.",e);}
        catch(IOException e){cause=e;}
        throw new IOException("Não foi possível abrir automaticamente. Configure um "+(browser?"navegador":"leitor de PDF ou gerenciador de arquivos")+" padrão e abra manualmente:\n"+target,cause);
    }
}
