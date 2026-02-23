"""add_play_purchase_columns

Revision ID: a3c7f29e1d45
Revises: 659b6e8212a6
Create Date: 2026-02-23 18:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'a3c7f29e1d45'
down_revision: Union[str, None] = '659b6e8212a6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column('users', sa.Column('play_purchase_token', sa.String(), nullable=True))
    op.add_column('users', sa.Column('pro_product_id', sa.String(), nullable=True))


def downgrade() -> None:
    op.drop_column('users', 'pro_product_id')
    op.drop_column('users', 'play_purchase_token')
