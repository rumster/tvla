#include <stdio.h>
typedef struct Tree {
  int marked, done, done_right;
  struct Tree *left, *right;
} Tree;

void DSW(Tree *x) {

  Tree *y = NULL, *t = NULL;
  if (x == NULL) return;
  x->marked = 1; 	

  while (1) {

    if (!x->done) {	

      if ( ! x->done_right) {

	y = x->right;

	if (y == NULL)
	  x->done_right = 1;

	else if (!y->marked) {
	  x->right = t; t = x; x = y; x->marked = 1;  //mark(x);
	}

	else
	  x->done_right = 1;
      }

      else {				
	y = x->left;

	if (y == NULL)
	  x->done = 1;

	else if (!y->marked) {
	  x->left = t; t = x; x = y; x->marked = 1; //mark(x);
	}

	else
		x->done = 1;
      }
    }

    else {

      y = x; x = t;

      if (x == NULL) 
	return;	

      else {					

	if (!x->done_right) {
	  t = x->right; x->right = y; 
	  x->done_right = 1;
	}

	else {
	  t = x->left; x->left = y; x->done = 1;
	}
      }
    }
  }
}

int count;

void PrintTree(Tree *t)
{
  count++;
  printf("count=%d,this=%p,marked=%d,done=%d,done_right=%d,left=%p,right=%p\n",
	 count,t, t->marked,t->done,t->done_right,t->left,t->right);
  if (t->left)
    PrintTree(t->left);
  if (t->right)
    PrintTree(t->right);
  printf("\n");
}

int main()
{
  Tree t1 = {0,0,0, NULL, NULL};
  Tree t2 = t1, t3 = t1;
  Tree t4 = {0, 0, 0, &t1, &t2};
  Tree tree = {0, 0, 0, &t4, &t3};

  count = 0;
  PrintTree(&tree);

  printf("Run DSW\n");
  DSW(&tree);
  printf("Finished DSW\n");

  count = 0;
  PrintTree(&tree);

  return 0;
}
