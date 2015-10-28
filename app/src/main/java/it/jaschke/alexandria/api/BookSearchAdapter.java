package it.jaschke.alexandria.api;


import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;

public class BookSearchAdapter extends CursorAdapter {


    public static class ViewHolder {
        public final TextView bookTitle;

        public ViewHolder(View view) {
            bookTitle = (TextView) view;
        }
    }

    public BookSearchAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        String bookTitle = cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        viewHolder.bookTitle.setText(bookTitle);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.simple_list_adapter, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if((!this.getCursor().isClosed()) && (!newCursor.isClosed()))
        return super.swapCursor(newCursor);
        else
            return null;
    }
}
