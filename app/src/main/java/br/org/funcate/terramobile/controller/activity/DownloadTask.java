package br.org.funcate.terramobile.controller.activity;

import android.os.AsyncTask;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import br.org.funcate.terramobile.R;
import br.org.funcate.terramobile.model.exception.DownloadException;

/**
 * This AsyncTask receives the data from the server
 */
public class DownloadTask extends AsyncTask<String, String, Boolean> {

    private String unzipDestinationFilePath;
    private String downloadDestinationFilePath;

    private DownloadException exception;

    private boolean overwrite;

    public MainActivity mainActivity;

    public DownloadTask(String unzipDestinationFilePath, boolean overwrite, MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.unzipDestinationFilePath = unzipDestinationFilePath;
        this.downloadDestinationFilePath = unzipDestinationFilePath + "/" + mainActivity.getResources().getString(R.string.destination_file_path);
        this.overwrite = overwrite;
    }

    @Override
    protected void onPreExecute() {
        mainActivity.showProgressDialog();
    }

    protected Boolean doInBackground(String... urlToDownload) {
        if (urlToDownload[0].isEmpty()) {
            exception = new DownloadException("Missing URL to be downloaded.");
            return false;
        }

        if (downloadDestinationFilePath.isEmpty()) {
            exception = new DownloadException("Missing destination path to download to.");
            return false;
        }

        try {
            try {
                File file = new File(downloadDestinationFilePath);

                if (!file.exists()) {
                    file.createNewFile();
                } else {
                    if (overwrite) {
                        file.delete();
                    } else {
                        return true;
                    }
                }
                URL url = new URL(urlToDownload[0]);

                URLConnection urlConnection = url.openConnection();
                urlConnection.connect();

                int totalSize = urlConnection.getContentLength();

                InputStream inputStream = new BufferedInputStream(url.openStream());

                OutputStream fileOutput = new FileOutputStream(file);

                byte buffer[] = new byte[1024];

                int bufferLength;

                long total = 0;

//                    if(android.os.Debug.isDebuggerConnected()) android.os.Debug.waitForDebugger(); Para debugar é preciso colocar um breakpoint nessa linha

                while ((bufferLength = inputStream.read(buffer)) != -1) {
                    total += bufferLength;
                    publishProgress("" + (int) ((total * 100) / totalSize), mainActivity.getResources().getString(R.string.downloding));

                    fileOutput.write(buffer, 0, bufferLength);
                }
                fileOutput.flush();

                fileOutput.close();

                this.unzip(new File(downloadDestinationFilePath), new File(unzipDestinationFilePath));

                return true;

            } catch (IOException e) {
                throw new DownloadException("Error downloading file: " + urlToDownload[0], e);
            }

        } catch (DownloadException e) {
            exception = e;
        }
        if(mainActivity.getProgressDialog() != null && mainActivity.getProgressDialog().isShowing())
            mainActivity.getProgressDialog().dismiss();
        return false;
    }

    /**
     * Count the number of files on a zip
     * @param zipFile Zip file
     * @return The number of files on the zip archive
     */
    private long countZipFiles(File zipFile){
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(zipFile)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        long totalFiles = 0;
        try {
            while (zis.getNextEntry() != null) {
                totalFiles++;
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  totalFiles;
    }

    /**
     * Unzip an archive
     * @param zipFile Zip archive
     * @param targetDirectory Directory to unzip the files
     * @throws IOException
     */
    public void unzip(File zipFile, File targetDirectory) throws IOException {
        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            int numFiles = 0;
            long totalFiles = countZipFiles(zipFile);

            while ((ze = zis.getNextEntry()) != null) {
                numFiles++;

                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    long total = 0;
                    long totalZipSize = ze.getCompressedSize();
                    while ((count = zis.read(buffer)) != -1) {
                        total += count;
                        publishProgress("" + (int) ((total * 100) / totalZipSize), mainActivity.getResources().getString(R.string.decompressing)+"\n"+mainActivity.getResources().getString(R.string.file) + " " + numFiles + "/" + totalFiles);
                        fout.write(buffer, 0, count);
                    }
                } finally {
                    fout.close();
                }
            }
        } finally {
            zis.close();
        }
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        mainActivity.getTreeView().refreshTreeView();
        if(mainActivity.getProgressDialog() != null && mainActivity.getProgressDialog().isShowing()) {
            if (aBoolean) {
                mainActivity.getProgressDialog().dismiss();
                Toast.makeText(mainActivity, R.string.download_success, Toast.LENGTH_LONG).show();
            } else {
                mainActivity.getProgressDialog().dismiss();
                Toast.makeText(mainActivity, R.string.download_failed, Toast.LENGTH_LONG).show();
            }
        }
        else{
            Toast.makeText(mainActivity, R.string.download_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if(mainActivity.getProgressDialog() != null && mainActivity.getProgressDialog().isShowing()) {
            mainActivity.getProgressDialog().setProgress(Integer.parseInt(values[0]));
            mainActivity.getProgressDialog().setMessage(values[1]);
        }
    }

    public DownloadException getException() {
        return exception;
    }
}