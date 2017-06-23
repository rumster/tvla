// a procedure which reverse a list.

List* hd ;
List* tl ;

List* reverse(List* r) {
  List *y ;
  List *t ;

  t = NULL;

  if (r != NULL) {    
    t = r->n ;     

    t = rev(t) ;    

    y = r->n; 
    r->n = NULL ;

    if (y != NULL) {
      y->n = NULL;
      y->n = r;
    }
    else
      t = r ;
  }

  return t ;

}
