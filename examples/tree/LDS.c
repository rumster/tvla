#include <stdio.h>
typedef struct Tree {
  int marked, done, done_right;
  struct Tree *left, *right;
} Tree;

// Lindstrom constant space traversal of a binary tree.
void Lindstrom(Tree *root) {

  Tree *prev, *cur, *next, *tmp;

  if (root == NULL) return;

  prev = SENTINEL;
  cur = root;

  while(1) {
    // Rotate pointers
    next = cur->left;
    cur->left = cur->right;
    cur->right = prev;

    // Do any work (marking, counting, etc.) on cur here.

    // Move forward.
    prev = cur;
    cur = next;

    if (cur == SENTINEL) break;

    // if (ATOM(cur))
    if (cur == NULL || cur->left == NULL && cur->right == NULL) {
      // Swap prev and cur.
      tmp = prev;
      prev = cur;
      cur = tmp;
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

  printf("Run LDS\n");
  Lindstrom(&tree);
  printf("Finished LDS\n");

  count = 0;
  PrintTree(&tree);

  return 0;
}
