#include <stdlib.h>

void exit(int s) {
 _EXIT: goto _EXIT;
}

typedef struct node {
  int h;
  struct node *n;
} *List;

void main() {
  int __BLAST_NONDET;
  
  /* Build a list of the form 1->...->1->2->....->2->3 */
  List a = (List) malloc(sizeof(struct node));
  if (a == 0) exit(1);
  List t;
  List p = a;
  while (__BLAST_NONDET) {
    p->h = 1;
    t = (List) malloc(sizeof(struct node));
    if (t == 0) exit(1);
    p->n = t;
    p = p->n;
  }
  p->h = 2;
    
  /* Check it */
  p = a;
  while (p->h == 1)
    p = p->n;
  if(p->h != 2)
    ERROR: goto ERROR;

  return 0;
}
