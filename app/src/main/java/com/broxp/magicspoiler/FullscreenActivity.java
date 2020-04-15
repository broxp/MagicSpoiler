package com.broxp.magicspoiler;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The main activity where user input and displaying images and text happens.
 *
 * @author broxp
 */
public class FullscreenActivity extends Activity {
    ImageView tile;
    ViewGroup grid;
    TextView text, titleText;
    int x, y;
    int lastX, lastY;
    Result result;
    Button info;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        try {
            super.onPostCreate(savedInstanceState);
            setContentView(R.layout.activity_fullscreen);

            grid = (ViewGroup) findViewById(R.id.grid);
            Log.d("grid", "" + grid);
            tile = (ImageView) findViewById(R.id.image);
            Log.d("tile", "" + tile);
            text = (TextView) findViewById(R.id.info_text);
            Log.d("tile", "" + tile);
            titleText = (TextView) findViewById(R.id.title);
            Log.d("text", "" + titleText);
            info = (Button) findViewById(R.id.info_button);
            Log.d("info", "" + info);

            Log.d("info", "RetrieveMagicTask");

            Rect rect = new Rect(0, 0, grid.getWidth(), 35);
            setTitleText("Loading spoilers...");

            AsyncTask<Void, Void, Result> execute = new RetrieveMagicTask()
                    .execute((Void) null);

            Exception exception;
            try {
                result = execute.get();
                exception = result.exception;
            } catch (Exception e) {
                exception = e;
            }
            if (result.exception != null) {
                handle(exception);
            }
            if (exception == null) {
                //setTitleText();
                showImage();
                info.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new StringDownloadTask() {
                            @Override
                            protected void onPostExecute(Object result) {
                                handleIfException(result);
                                if (result instanceof String) {
                                    text.setVisibility(View.VISIBLE);
                                    String html = (String) result;
                                    setCardInfo(html);
                                }
                            }
                        }.execute(currentCard().pageUrl);
                    }
                });

                rect = new Rect(0, 0, grid.getWidth(), grid.getHeight());
                grid.setTouchDelegate(new TouchDelegate(rect, grid) {
                    @Override
                    public boolean onTouchEvent(MotionEvent event) {
                        touch(event);
                        text.setVisibility(View.GONE);
                        return super.onTouchEvent(event);
                    }
                });
            }
        } catch (Exception ex) {
            Toast.makeText(this,
                    ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
            ;
            // handle(ex);
        }
    }

    private void setTitleText(String s) {
        titleText.setText(s);
    }

    private void setCardInfo(String result2) {
        String searchStart = "<tr><td colspan=\"2\" valign=\"top\"><font size=\"4\">";
        String searchEnd = "</td></tr></table>";
        int start = -1;
        int end = -1;
        if ((start = result2.indexOf(searchStart)) >= 0) {
            start += searchStart.length();
            if ((end = result2.indexOf(searchEnd, start)) >= 0) {
                end += searchEnd.length();
                String substring = result2.substring(start, end);
                StringBuilder sb = new StringBuilder();
                boolean inTag = false;
                for (int i = 0; i < substring.length(); i++) {
                    char c = substring.charAt(i);
                    if (c == '<') {
                        inTag = true;
                    } else if (c == '>') {
                        inTag = false;
                    }
                    if (!inTag && c != '>') {
                        sb.append(c);
                    }
                }
                text.setText(sb.toString().replace("\r", "").replace("\n\n", "\n"));
            }
        }
    }

    void handle(Exception exception) {
        Toast.makeText(this,
                exception.getClass().getSimpleName() + ": " + exception.getLocalizedMessage(),
                Toast.LENGTH_LONG).show();

        Log.e("exception", exception + "");
        String string = exception.getClass().getSimpleName() + ": "
                + exception.getLocalizedMessage();
        setTitleText(string);
        text.setVisibility(View.VISIBLE);
        text.setText(string);
    }

    void touch(MotionEvent event) {
        float w = grid.getWidth();
        float h = grid.getHeight();
        float xMouse = event.getX();
        float yMouse = event.getY();
        float dx = xMouse - w / 2;
        float dy = yMouse - h / 2;
        x += Math.abs(dx) < w * 0.1 ? 0 : Math.signum(dx);
        y += Math.abs(dy) < h * 0.1 ? 0 : Math.signum(dy);
        int max = max();
        if (x >= max) {
            x = 0;
        } else if (x < 0) {
            x = max - 1;
        }
        if (y >= max) {
            y = 0;
        } else if (y < 0) {
            y = max - 1;
        }

        if (x != lastX || y != lastY) {
            showImage();
            lastX = x;
            lastY = y;
        }
    }

    Card currentCard() {
        return result.imgs.get(x);
    }

    void showImage() {
        setTitleText((x + 1) + "/" + max() + " loading..." + (result == null ? "" : result.title));
        String current = currentCard().imageUrl;
        new DownloadImageTask() {
            @Override
            protected void onPostExecute(Object result) {
                handleIfException(result);
                if (result instanceof Bitmap) {
                    tile.setImageBitmap((Bitmap) result);
                    status();
                }
            }
        }.execute(current);
    }

    void handleIfException(Object result) {
        if (result instanceof Exception) {
            handle((Exception) result);
        }
    }

    void status() {
        setTitleText((x + 1) + "/" + max() + " " + (result == null ? "" : result.title));
    }

    private int max() {
        return result.imgs.size();
    }
}
