#include "tree.h"
#include <stdio.h>

void foo(Tree *root) {
  Tree *t;
  
  if (root != NULL) {
    t = root->right ;
    root->left= t; 
  }
}
