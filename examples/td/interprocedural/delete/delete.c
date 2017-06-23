// a procedure which deletes an element from an keyed list.

List* hd ;
List* delete(List* h /*, int k */ ) {
  List* t ;

  if (h == NULL) 
    return NULL;

  t = h->n ;

  if ( ? /* h->d == k */ ) {
    h->n = NULL ;
    free (h) ;
    return t ;
  }

  t = delete (t /*,k */ ) ;
  h->n = NULL;
  h->n = t ;
  return h ;
}
