// a procedure which inserts an element into an ordered list.

List* hd1 ;
List* hd2 ;

List* merge(List* p , List* q /*, int k */ ) {
  List* t ;
  List* r ;

  if (p == NULL) 
	  return q;

  if (q == NULL) 
	  return p ;


  if ( ? /* p->d > q->d */ )  {
	  t = p->n;
    p->n = NULL;

	  t = merge(t,q);
	  r = p;	  		
  }
  else {
	  t = q->n;
    q->n = NULL;

	  t = merge(p,t);
	  r = q;
  }

  r->n = t;
   
  return r  ;
}


