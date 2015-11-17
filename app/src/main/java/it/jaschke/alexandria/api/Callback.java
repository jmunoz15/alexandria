package it.jaschke.alexandria.api;

public interface Callback {
    void onItemSelected(String ean);

    void onItemRemoved(String ean);
}
