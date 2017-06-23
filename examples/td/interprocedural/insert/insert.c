// a procedure which inserts an element into an ordered list.

List* hd ;
List* insert(List* h /*, int k */ ) {
  List* t ;

  if (h != NULL && ? /* h->d < k */ ) {
    t = h->n ;
    t = insert (t /*,k */ ) ;
    h->n = NULL;
    h->n = t ;
    return h ;
   }

   t = malloc(sizeof(List)); 
   t->n = h ; // h becomes shared !
   // t->d = k; 
   return t  ;
}


